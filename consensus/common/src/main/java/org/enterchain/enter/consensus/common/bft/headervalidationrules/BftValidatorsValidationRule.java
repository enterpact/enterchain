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
package org.enterchain.enter.consensus.common.bft.headervalidationrules;

import org.enterchain.enter.consensus.common.ValidatorProvider;
import org.enterchain.enter.consensus.common.bft.BftContext;
import org.enterchain.enter.consensus.common.bft.BftExtraData;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.mainnet.AttachedBlockHeaderValidationRule;
import org.enterchain.enter.ethereum.rlp.RLPException;

import java.util.Collection;
import java.util.NavigableSet;
import java.util.TreeSet;

import com.google.common.collect.Iterables;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ensures the Validators listed in the block header match that tracked in memory (which was in-turn
 * created by tracking votes included on the block chain).
 */
public class BftValidatorsValidationRule implements AttachedBlockHeaderValidationRule {

  private static final Logger LOGGER = LogManager.getLogger();

  @Override
  public boolean validate(
      final BlockHeader header, final BlockHeader parent, final ProtocolContext context) {
    try {
      final BftContext bftContext = context.getConsensusState(BftContext.class);
      final ValidatorProvider validatorProvider =
          bftContext.getVoteTallyCache().getVoteTallyAfterBlock(parent);
      final BftExtraData bftExtraData = bftContext.getBlockInterface().getExtraData(header);

      final NavigableSet<Address> sortedReportedValidators =
          new TreeSet<>(bftExtraData.getValidators());

      if (!Iterables.elementsEqual(bftExtraData.getValidators(), sortedReportedValidators)) {
        LOGGER.info(
            "Invalid block header: Validators are not sorted in ascending order. Expected {} but got {}.",
            sortedReportedValidators,
            bftExtraData.getValidators());
        return false;
      }

      final Collection<Address> storedValidators = validatorProvider.getValidators();
      if (!Iterables.elementsEqual(bftExtraData.getValidators(), storedValidators)) {
        LOGGER.info(
            "Invalid block header: Incorrect validators. Expected {} but got {}.",
            storedValidators,
            bftExtraData.getValidators());
        return false;
      }

    } catch (final RLPException ex) {
      LOGGER.info(
          "Invalid block header: ExtraData field was unable to be deserialized into an BFT Struct.",
          ex);
      return false;
    } catch (final IllegalArgumentException ex) {
      LOGGER.info("Invalid block header: Failed to verify extra data", ex);
      return false;
    }

    return true;
  }
}
