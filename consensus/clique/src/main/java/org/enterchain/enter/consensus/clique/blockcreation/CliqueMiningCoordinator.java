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
package org.enterchain.enter.consensus.clique.blockcreation;

import static org.apache.logging.log4j.LogManager.getLogger;

import org.enterchain.enter.consensus.clique.CliqueMiningTracker;
import org.enterchain.enter.ethereum.blockcreation.AbstractMiningCoordinator;
import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.eth.sync.state.SyncState;

import org.apache.logging.log4j.Logger;

public class CliqueMiningCoordinator extends AbstractMiningCoordinator<CliqueBlockMiner> {

  private static final Logger LOG = getLogger();

  private final CliqueMiningTracker miningTracker;

  public CliqueMiningCoordinator(
      final Blockchain blockchain,
      final CliqueMinerExecutor executor,
      final SyncState syncState,
      final CliqueMiningTracker miningTracker) {
    super(blockchain, executor, syncState);
    this.miningTracker = miningTracker;
  }

  @Override
  public void onResumeMining() {
    if (isSigner()) {
      LOG.info("Resuming block production operations");
    }
  }

  @Override
  public void onPauseMining() {
    if (isSigner()) {
      LOG.info("Pausing block production while behind chain head");
    }
  }

  public boolean isSigner() {
    return miningTracker.isSigner(blockchain.getChainHeadHeader());
  }

  @Override
  protected boolean newChainHeadInvalidatesMiningOperation(final BlockHeader newChainHeadHeader) {
    if (currentRunningMiner.isEmpty()) {
      return true;
    }

    if (miningTracker.blockCreatedLocally(newChainHeadHeader)) {
      return true;
    }

    return networkBlockBetterThanCurrentMiner(newChainHeadHeader);
  }

  private boolean networkBlockBetterThanCurrentMiner(final BlockHeader newChainHeadHeader) {
    final BlockHeader parentHeader = currentRunningMiner.get().getParentHeader();
    final long currentMinerTargetHeight = parentHeader.getNumber() + 1;
    if (currentMinerTargetHeight < newChainHeadHeader.getNumber()) {
      return true;
    }

    final boolean nodeIsMining = miningTracker.canMakeBlockNextRound(parentHeader);
    final boolean nodeIsInTurn = miningTracker.isProposerAfter(parentHeader);

    return !nodeIsMining || !nodeIsInTurn;
  }
}
