/*
 * Copyright 2020 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.enterchain.enter.consensus.qbft.validation;

import static org.enterchain.enter.consensus.common.bft.validation.ValidationHelpers.hasDuplicateAuthors;
import static org.enterchain.enter.consensus.common.bft.validation.ValidationHelpers.hasSufficientEntries;

import org.enterchain.enter.consensus.common.bft.ConsensusRoundIdentifier;
import org.enterchain.enter.consensus.common.bft.payload.SignedData;
import org.enterchain.enter.consensus.qbft.messagewrappers.RoundChange;
import org.enterchain.enter.consensus.qbft.payload.PreparePayload;
import org.enterchain.enter.consensus.qbft.payload.PreparedRoundMetadata;
import org.enterchain.enter.ethereum.BlockValidator;
import org.enterchain.enter.ethereum.BlockValidator.BlockProcessingOutputs;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.mainnet.HeaderValidationMode;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RoundChangeMessageValidator {

  private static final String ERROR_PREFIX = "Invalid RoundChange Message";

  private static final Logger LOG = LogManager.getLogger();

  private final RoundChangePayloadValidator roundChangePayloadValidator;
  private final long quorumMessageCount;
  private final long chainHeight;
  private final Collection<Address> validators;
  private final BlockValidator blockValidator;
  private final ProtocolContext protocolContext;

  public RoundChangeMessageValidator(
      final RoundChangePayloadValidator roundChangePayloadValidator,
      final long quorumMessageCount,
      final long chainHeight,
      final Collection<Address> validators,
      final BlockValidator blockValidator,
      final ProtocolContext protocolContext) {
    this.roundChangePayloadValidator = roundChangePayloadValidator;
    this.quorumMessageCount = quorumMessageCount;
    this.chainHeight = chainHeight;
    this.validators = validators;
    this.blockValidator = blockValidator;
    this.protocolContext = protocolContext;
  }

  public boolean validate(final RoundChange msg) {

    if (!roundChangePayloadValidator.validate(msg.getSignedPayload())) {
      LOG.info("{}: embedded payload was invalid", ERROR_PREFIX);
      return false;
    }

    if (msg.getProposedBlock().isPresent()) {
      return validateWithBlock(msg);
    }

    return msg.getPreparedRoundMetadata().isEmpty();
  }

  private boolean validateBlock(final Block block) {
    final Optional<BlockProcessingOutputs> validationResult =
        blockValidator.validateAndProcessBlock(
            protocolContext, block, HeaderValidationMode.LIGHT, HeaderValidationMode.FULL);

    if (validationResult.isEmpty()) {
      LOG.info("{}: block did not pass validation.", ERROR_PREFIX);
      return false;
    }

    return true;
  }

  private boolean validateWithBlock(final RoundChange msg) {
    final Block block = msg.getProposedBlock().get();

    if (!validateBlock(block)) {
      return false;
    }

    if (msg.getPreparedRoundMetadata().isEmpty()) {
      LOG.info("{}: Prepared block specified, but prepared metadata absent", ERROR_PREFIX);
      return false;
    }

    final PreparedRoundMetadata metadata = msg.getPreparedRoundMetadata().get();

    if (!metadata.getPreparedBlockHash().equals(block.getHash())) {
      LOG.info("{}: Prepared metadata hash does not match supplied block", ERROR_PREFIX);
      return false;
    }

    return validatePrepares(metadata, msg.getPrepares());
  }

  private boolean validatePrepares(
      final PreparedRoundMetadata metaData, final List<SignedData<PreparePayload>> prepares) {

    final ConsensusRoundIdentifier preparedRoundIdentifier =
        new ConsensusRoundIdentifier(chainHeight, metaData.getPreparedRound());

    final PrepareValidator validator =
        new PrepareValidator(validators, preparedRoundIdentifier, metaData.getPreparedBlockHash());

    if (hasDuplicateAuthors(prepares)) {
      LOG.info("{}: multiple prepares from the same author.", ERROR_PREFIX);
      return false;
    }

    if (!hasSufficientEntries(prepares, quorumMessageCount)) {
      LOG.info("{}: insufficient Prepare messages piggybacked.", ERROR_PREFIX);
      return false;
    }

    return prepares.stream().allMatch(validator::validate);
  }
}
