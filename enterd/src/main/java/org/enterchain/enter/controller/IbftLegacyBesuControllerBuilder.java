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

import org.enterchain.enter.config.IbftLegacyConfigOptions;
import org.enterchain.enter.consensus.common.BlockInterface;
import org.enterchain.enter.consensus.common.EpochManager;
import org.enterchain.enter.consensus.common.VoteProposer;
import org.enterchain.enter.consensus.common.VoteTallyCache;
import org.enterchain.enter.consensus.common.VoteTallyUpdater;
import org.enterchain.enter.consensus.ibft.IbftLegacyContext;
import org.enterchain.enter.consensus.ibftlegacy.IbftLegacyBlockInterface;
import org.enterchain.enter.consensus.ibftlegacy.IbftProtocolSchedule;
import org.enterchain.enter.consensus.ibftlegacy.protocol.Istanbul99Protocol;
import org.enterchain.enter.consensus.ibftlegacy.protocol.Istanbul99ProtocolManager;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.blockcreation.MiningCoordinator;
import org.enterchain.enter.ethereum.blockcreation.NoopMiningCoordinator;
import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.MiningParameters;
import org.enterchain.enter.ethereum.eth.EthProtocolConfiguration;
import org.enterchain.enter.ethereum.eth.manager.EthContext;
import org.enterchain.enter.ethereum.eth.manager.EthMessages;
import org.enterchain.enter.ethereum.eth.manager.EthPeers;
import org.enterchain.enter.ethereum.eth.manager.EthProtocolManager;
import org.enterchain.enter.ethereum.eth.manager.EthScheduler;
import org.enterchain.enter.ethereum.eth.peervalidation.PeerValidator;
import org.enterchain.enter.ethereum.eth.sync.state.SyncState;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPool;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.p2p.config.SubProtocolConfiguration;
import org.enterchain.enter.ethereum.worldstate.WorldStateArchive;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IbftLegacyBesuControllerBuilder extends BesuControllerBuilder {

  private static final Logger LOG = LogManager.getLogger();
  private final BlockInterface blockInterface = new IbftLegacyBlockInterface();

  @Override
  protected SubProtocolConfiguration createSubProtocolConfiguration(
      final EthProtocolManager ethProtocolManager) {
    return new SubProtocolConfiguration()
        .withSubProtocol(Istanbul99Protocol.get(), ethProtocolManager);
  }

  @Override
  protected MiningCoordinator createMiningCoordinator(
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final TransactionPool transactionPool,
      final MiningParameters miningParameters,
      final SyncState syncState,
      final EthProtocolManager ethProtocolManager) {
    return new NoopMiningCoordinator(miningParameters);
  }

  @Override
  protected ProtocolSchedule createProtocolSchedule() {
    return IbftProtocolSchedule.create(
        genesisConfig.getConfigOptions(genesisConfigOverrides),
        privacyParameters,
        isRevertReasonEnabled);
  }

  @Override
  protected IbftLegacyContext createConsensusContext(
      final Blockchain blockchain, final WorldStateArchive worldStateArchive) {
    final IbftLegacyConfigOptions ibftConfig =
        genesisConfig.getConfigOptions(genesisConfigOverrides).getIbftLegacyConfigOptions();
    final EpochManager epochManager = new EpochManager(ibftConfig.getEpochLength());
    final VoteTallyCache voteTallyCache =
        new VoteTallyCache(
            blockchain,
            new VoteTallyUpdater(epochManager, blockInterface),
            epochManager,
            blockInterface);

    final VoteProposer voteProposer = new VoteProposer();

    return new IbftLegacyContext(voteTallyCache, voteProposer, epochManager, blockInterface);
  }

  @Override
  protected PluginServiceFactory createAdditionalPluginServices(final Blockchain blockchain) {
    return new NoopPluginServiceFactory();
  }

  @Override
  protected void validateContext(final ProtocolContext context) {
    final BlockHeader genesisBlockHeader = context.getBlockchain().getGenesisBlock().getHeader();

    if (blockInterface.validatorsInBlock(genesisBlockHeader).isEmpty()) {
      LOG.warn("Genesis block contains no signers - chain will not progress.");
    }
  }

  @Override
  protected String getSupportedProtocol() {
    return Istanbul99Protocol.get().getName();
  }

  @Override
  protected EthProtocolManager createEthProtocolManager(
      final ProtocolContext protocolContext,
      final boolean fastSyncEnabled,
      final TransactionPool transactionPool,
      final EthProtocolConfiguration ethereumWireProtocolConfiguration,
      final EthPeers ethPeers,
      final EthContext ethContext,
      final EthMessages ethMessages,
      final EthScheduler scheduler,
      final List<PeerValidator> peerValidators) {
    LOG.info("Operating on IBFT-1.0 network.");
    return new Istanbul99ProtocolManager(
        protocolContext.getBlockchain(),
        networkId,
        protocolContext.getWorldStateArchive(),
        transactionPool,
        ethereumWireProtocolConfiguration,
        ethPeers,
        ethMessages,
        ethContext,
        peerValidators,
        fastSyncEnabled,
        scheduler);
  }
}
