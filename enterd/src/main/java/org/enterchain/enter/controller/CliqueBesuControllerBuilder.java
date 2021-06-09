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

import org.enterchain.enter.config.CliqueConfigOptions;
import org.enterchain.enter.consensus.clique.CliqueBlockInterface;
import org.enterchain.enter.consensus.clique.CliqueContext;
import org.enterchain.enter.consensus.clique.CliqueMiningTracker;
import org.enterchain.enter.consensus.clique.CliqueProtocolSchedule;
import org.enterchain.enter.consensus.clique.blockcreation.CliqueBlockScheduler;
import org.enterchain.enter.consensus.clique.blockcreation.CliqueMinerExecutor;
import org.enterchain.enter.consensus.clique.blockcreation.CliqueMiningCoordinator;
import org.enterchain.enter.consensus.clique.jsonrpc.CliqueJsonRpcMethods;
import org.enterchain.enter.consensus.common.BlockInterface;
import org.enterchain.enter.consensus.common.EpochManager;
import org.enterchain.enter.consensus.common.VoteProposer;
import org.enterchain.enter.consensus.common.VoteTallyCache;
import org.enterchain.enter.consensus.common.VoteTallyUpdater;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.api.jsonrpc.methods.JsonRpcMethods;
import org.enterchain.enter.ethereum.blockcreation.MiningCoordinator;
import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.MiningParameters;
import org.enterchain.enter.ethereum.core.Util;
import org.enterchain.enter.ethereum.eth.manager.EthProtocolManager;
import org.enterchain.enter.ethereum.eth.sync.state.SyncState;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPool;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.worldstate.WorldStateArchive;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CliqueBesuControllerBuilder extends BesuControllerBuilder {

  private static final Logger LOG = LogManager.getLogger();

  private Address localAddress;
  private EpochManager epochManager;
  private long secondsBetweenBlocks;
  private final BlockInterface blockInterface = new CliqueBlockInterface();

  @Override
  protected void prepForBuild() {
    localAddress = Util.publicKeyToAddress(nodeKey.getPublicKey());
    final CliqueConfigOptions cliqueConfig =
        genesisConfig.getConfigOptions(genesisConfigOverrides).getCliqueConfigOptions();
    final long blocksPerEpoch = cliqueConfig.getEpochLength();
    secondsBetweenBlocks = cliqueConfig.getBlockPeriodSeconds();

    epochManager = new EpochManager(blocksPerEpoch);
  }

  @Override
  protected JsonRpcMethods createAdditionalJsonRpcMethodFactory(
      final ProtocolContext protocolContext) {
    return new CliqueJsonRpcMethods(protocolContext);
  }

  @Override
  protected MiningCoordinator createMiningCoordinator(
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final TransactionPool transactionPool,
      final MiningParameters miningParameters,
      final SyncState syncState,
      final EthProtocolManager ethProtocolManager) {
    final CliqueMinerExecutor miningExecutor =
        new CliqueMinerExecutor(
            protocolContext,
            protocolSchedule,
            transactionPool.getPendingTransactions(),
            nodeKey,
            miningParameters,
            new CliqueBlockScheduler(
                clock,
                protocolContext.getConsensusState(CliqueContext.class).getVoteTallyCache(),
                localAddress,
                secondsBetweenBlocks),
            epochManager,
            gasLimitCalculator);
    final CliqueMiningCoordinator miningCoordinator =
        new CliqueMiningCoordinator(
            protocolContext.getBlockchain(),
            miningExecutor,
            syncState,
            new CliqueMiningTracker(localAddress, protocolContext));
    miningCoordinator.addMinedBlockObserver(ethProtocolManager);

    // Clique mining is implicitly enabled.
    miningCoordinator.enable();
    return miningCoordinator;
  }

  @Override
  protected ProtocolSchedule createProtocolSchedule() {
    return CliqueProtocolSchedule.create(
        genesisConfig.getConfigOptions(genesisConfigOverrides),
        nodeKey,
        privacyParameters,
        isRevertReasonEnabled);
  }

  @Override
  protected void validateContext(final ProtocolContext context) {
    final BlockHeader genesisBlockHeader = context.getBlockchain().getGenesisBlock().getHeader();

    if (blockInterface.validatorsInBlock(genesisBlockHeader).isEmpty()) {
      LOG.warn("Genesis block contains no signers - chain will not progress.");
    }
  }

  @Override
  protected PluginServiceFactory createAdditionalPluginServices(final Blockchain blockchain) {
    return new CliqueQueryPluginServiceFactory(blockchain, nodeKey);
  }

  @Override
  protected CliqueContext createConsensusContext(
      final Blockchain blockchain, final WorldStateArchive worldStateArchive) {
    return new CliqueContext(
        new VoteTallyCache(
            blockchain,
            new VoteTallyUpdater(epochManager, blockInterface),
            epochManager,
            blockInterface),
        new VoteProposer(),
        epochManager,
        blockInterface);
  }
}
