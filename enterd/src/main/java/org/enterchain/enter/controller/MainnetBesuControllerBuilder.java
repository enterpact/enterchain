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
package org.enterchain.enter.controller;

import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.blockcreation.DefaultBlockScheduler;
import org.enterchain.enter.ethereum.blockcreation.MiningCoordinator;
import org.enterchain.enter.ethereum.blockcreation.PoWMinerExecutor;
import org.enterchain.enter.ethereum.blockcreation.PoWMiningCoordinator;
import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.core.MiningParameters;
import org.enterchain.enter.ethereum.eth.manager.EthProtocolManager;
import org.enterchain.enter.ethereum.eth.sync.state.SyncState;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPool;
import org.enterchain.enter.ethereum.mainnet.EpochCalculator;
import org.enterchain.enter.ethereum.mainnet.MainnetBlockHeaderValidator;
import org.enterchain.enter.ethereum.mainnet.MainnetProtocolSchedule;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.worldstate.WorldStateArchive;

public class MainnetBesuControllerBuilder extends BesuControllerBuilder {

  private EpochCalculator epochCalculator = new EpochCalculator.DefaultEpochCalculator();

  @Override
  protected MiningCoordinator createMiningCoordinator(
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final TransactionPool transactionPool,
      final MiningParameters miningParameters,
      final SyncState syncState,
      final EthProtocolManager ethProtocolManager) {

    final PoWMinerExecutor executor =
        new PoWMinerExecutor(
            protocolContext,
            protocolSchedule,
            transactionPool.getPendingTransactions(),
            miningParameters,
            new DefaultBlockScheduler(
                MainnetBlockHeaderValidator.MINIMUM_SECONDS_SINCE_PARENT,
                MainnetBlockHeaderValidator.TIMESTAMP_TOLERANCE_S,
                clock),
            gasLimitCalculator,
            epochCalculator);

    final PoWMiningCoordinator miningCoordinator =
        new PoWMiningCoordinator(
            protocolContext.getBlockchain(),
            executor,
            syncState,
            miningParameters.getRemoteSealersLimit(),
            miningParameters.getRemoteSealersTimeToLive());
    miningCoordinator.addMinedBlockObserver(ethProtocolManager);
    miningCoordinator.setStratumMiningEnabled(miningParameters.isStratumMiningEnabled());
    if (miningParameters.isMiningEnabled()) {
      miningCoordinator.enable();
    }

    return miningCoordinator;
  }

  @Override
  protected Void createConsensusContext(
      final Blockchain blockchain, final WorldStateArchive worldStateArchive) {
    return null;
  }

  @Override
  protected PluginServiceFactory createAdditionalPluginServices(final Blockchain blockchain) {
    return new NoopPluginServiceFactory();
  }

  @Override
  protected ProtocolSchedule createProtocolSchedule() {
    return MainnetProtocolSchedule.fromConfig(
        genesisConfig.getConfigOptions(genesisConfigOverrides),
        privacyParameters,
        isRevertReasonEnabled);
  }

  @Override
  protected void prepForBuild() {
    genesisConfig
        .getConfigOptions()
        .getThanosBlockNumber()
        .ifPresent(
            activationBlock -> epochCalculator = new EpochCalculator.Ecip1099EpochCalculator());
  }
}
