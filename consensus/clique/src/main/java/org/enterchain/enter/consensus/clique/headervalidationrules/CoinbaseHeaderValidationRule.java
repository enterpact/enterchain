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
package org.enterchain.enter.consensus.clique.headervalidationrules;

import org.enterchain.enter.consensus.clique.CliqueBlockInterface;
import org.enterchain.enter.consensus.common.EpochManager;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.mainnet.DetachedBlockHeaderValidationRule;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CoinbaseHeaderValidationRule implements DetachedBlockHeaderValidationRule {

  private static final Logger LOG = LogManager.getLogger();

  private final EpochManager epochManager;

  public CoinbaseHeaderValidationRule(final EpochManager epochManager) {
    this.epochManager = epochManager;
  }

  @Override
  // The coinbase field is used for voting nodes in/out of the validator group. However, no votes
  // are allowed to be cast on epoch blocks
  public boolean validate(final BlockHeader header, final BlockHeader parent) {
    if (epochManager.isEpochBlock(header.getNumber())
        && !header.getCoinbase().equals(CliqueBlockInterface.NO_VOTE_SUBJECT)) {
      LOG.info(
          "Invalid block header: No clique in/out voting may occur on epoch blocks ({})",
          header.getNumber());
      return false;
    }
    return true;
  }
}
