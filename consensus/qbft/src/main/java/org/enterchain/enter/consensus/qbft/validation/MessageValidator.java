/*
 * Copyright ConsenSys AG.
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

import org.enterchain.enter.consensus.common.bft.BftBlockHeaderFunctions;
import org.enterchain.enter.consensus.common.bft.BftBlockInterface;
import org.enterchain.enter.consensus.common.bft.ConsensusRoundIdentifier;
import org.enterchain.enter.consensus.qbft.QbftExtraDataCodec;
import org.enterchain.enter.consensus.qbft.messagewrappers.Commit;
import org.enterchain.enter.consensus.qbft.messagewrappers.Prepare;
import org.enterchain.enter.consensus.qbft.messagewrappers.Proposal;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.Block;

import java.util.Collection;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MessageValidator {

  private static final Logger LOG = LogManager.getLogger();

  public static class SubsequentMessageValidator {

    private final PrepareValidator prepareValidator;
    private final CommitValidator commitValidator;

    public SubsequentMessageValidator(
        final Collection<Address> validators,
        final ConsensusRoundIdentifier targetRound,
        final Block proposalBlock,
        final BftBlockInterface blockInterface) {
      final Block commitBlock =
          blockInterface.replaceRoundInBlock(
              proposalBlock,
              targetRound.getRoundNumber(),
              BftBlockHeaderFunctions.forCommittedSeal(new QbftExtraDataCodec()));
      prepareValidator = new PrepareValidator(validators, targetRound, proposalBlock.getHash());
      commitValidator =
          new CommitValidator(
              validators, targetRound, proposalBlock.getHash(), commitBlock.getHash());
    }

    public boolean validate(final Prepare msg) {
      return prepareValidator.validate(msg);
    }

    public boolean validate(final Commit msg) {
      return commitValidator.validate(msg);
    }
  }

  @FunctionalInterface
  public interface SubsequentMessageValidatorFactory {
    SubsequentMessageValidator create(Block proposalBlock);
  }

  private final SubsequentMessageValidatorFactory subsequentMessageValidatorFactory;
  private final ProposalValidator proposalValidator;

  private Optional<SubsequentMessageValidator> subsequentMessageValidator = Optional.empty();

  public MessageValidator(
      final SubsequentMessageValidatorFactory subsequentMessageValidatorFactory,
      final ProposalValidator proposalValidator) {
    this.subsequentMessageValidatorFactory = subsequentMessageValidatorFactory;
    this.proposalValidator = proposalValidator;
  }

  public boolean validateProposal(final Proposal msg) {
    if (subsequentMessageValidator.isPresent()) {
      LOG.info("Received subsequent Proposal for current round, discarding.");
      return false;
    }

    final boolean result = proposalValidator.validate(msg);
    if (result) {
      subsequentMessageValidator =
          Optional.of(subsequentMessageValidatorFactory.create(msg.getBlock()));
    }

    return result;
  }

  public boolean validatePrepare(final Prepare msg) {
    return subsequentMessageValidator.map(pv -> pv.validate(msg)).orElse(false);
  }

  public boolean validateCommit(final Commit msg) {
    return subsequentMessageValidator.map(cv -> cv.validate(msg)).orElse(false);
  }
}
