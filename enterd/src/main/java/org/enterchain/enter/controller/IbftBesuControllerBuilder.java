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

import org.enterchain.enter.config.BftConfigOptions;
import org.enterchain.enter.config.BftFork;
import org.enterchain.enter.config.GenesisConfigOptions;
import org.enterchain.enter.consensus.common.BftValidatorOverrides;
import org.enterchain.enter.consensus.common.EpochManager;
import org.enterchain.enter.consensus.common.ForkingVoteTallyCache;
import org.enterchain.enter.consensus.common.VoteProposer;
import org.enterchain.enter.consensus.common.VoteTallyCache;
import org.enterchain.enter.consensus.common.VoteTallyUpdater;
import org.enterchain.enter.consensus.common.bft.BftBlockInterface;
import org.enterchain.enter.consensus.common.bft.BftContext;
import org.enterchain.enter.consensus.common.bft.BftEventQueue;
import org.enterchain.enter.consensus.common.bft.BftExecutors;
import org.enterchain.enter.consensus.common.bft.BftExtraDataCodec;
import org.enterchain.enter.consensus.common.bft.BftProcessor;
import org.enterchain.enter.consensus.common.bft.BftProtocolSchedule;
import org.enterchain.enter.consensus.common.bft.BlockTimer;
import org.enterchain.enter.consensus.common.bft.EthSynchronizerUpdater;
import org.enterchain.enter.consensus.common.bft.EventMultiplexer;
import org.enterchain.enter.consensus.common.bft.MessageTracker;
import org.enterchain.enter.consensus.common.bft.RoundTimer;
import org.enterchain.enter.consensus.common.bft.UniqueMessageMulticaster;
import org.enterchain.enter.consensus.common.bft.blockcreation.BftBlockCreatorFactory;
import org.enterchain.enter.consensus.common.bft.blockcreation.BftMiningCoordinator;
import org.enterchain.enter.consensus.common.bft.blockcreation.ProposerSelector;
import org.enterchain.enter.consensus.common.bft.network.ValidatorPeers;
import org.enterchain.enter.consensus.common.bft.protocol.BftProtocolManager;
import org.enterchain.enter.consensus.common.bft.statemachine.BftEventHandler;
import org.enterchain.enter.consensus.common.bft.statemachine.BftFinalState;
import org.enterchain.enter.consensus.common.bft.statemachine.FutureMessageBuffer;
import org.enterchain.enter.consensus.ibft.IbftBlockHeaderValidationRulesetFactory;
import org.enterchain.enter.consensus.ibft.IbftExtraDataCodec;
import org.enterchain.enter.consensus.ibft.IbftGossip;
import org.enterchain.enter.consensus.ibft.jsonrpc.IbftJsonRpcMethods;
import org.enterchain.enter.consensus.ibft.payload.MessageFactory;
import org.enterchain.enter.consensus.ibft.protocol.IbftSubProtocol;
import org.enterchain.enter.consensus.ibft.statemachine.IbftBlockHeightManagerFactory;
import org.enterchain.enter.consensus.ibft.statemachine.IbftController;
import org.enterchain.enter.consensus.ibft.statemachine.IbftRoundFactory;
import org.enterchain.enter.consensus.ibft.validation.MessageValidatorFactory;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.api.jsonrpc.methods.JsonRpcMethods;
import org.enterchain.enter.ethereum.blockcreation.MiningCoordinator;
import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.chain.MinedBlockObserver;
import org.enterchain.enter.ethereum.chain.MutableBlockchain;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.MiningParameters;
import org.enterchain.enter.ethereum.core.Util;
import org.enterchain.enter.ethereum.eth.EthProtocol;
import org.enterchain.enter.ethereum.eth.manager.EthProtocolManager;
import org.enterchain.enter.ethereum.eth.sync.state.SyncState;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPool;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.p2p.config.SubProtocolConfiguration;
import org.enterchain.enter.ethereum.worldstate.WorldStateArchive;
import org.enterchain.enter.util.Subscribers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IbftBesuControllerBuilder extends BesuControllerBuilder {

  private static final Logger LOG = LogManager.getLogger();
  private BftEventQueue bftEventQueue;
  private BftConfigOptions bftConfig;
  private ValidatorPeers peers;
  private final BftExtraDataCodec bftExtraDataCodec = new IbftExtraDataCodec();
  private final BftBlockInterface blockInterface = new BftBlockInterface(bftExtraDataCodec);

  @Override
  protected void prepForBuild() {
    bftConfig = genesisConfig.getConfigOptions(genesisConfigOverrides).getBftConfigOptions();
    bftEventQueue = new BftEventQueue(bftConfig.getMessageQueueLimit());
  }

  @Override
  protected JsonRpcMethods createAdditionalJsonRpcMethodFactory(
      final ProtocolContext protocolContext) {
    return new IbftJsonRpcMethods(protocolContext);
  }

  @Override
  protected SubProtocolConfiguration createSubProtocolConfiguration(
      final EthProtocolManager ethProtocolManager) {
    return new SubProtocolConfiguration()
        .withSubProtocol(EthProtocol.get(), ethProtocolManager)
        .withSubProtocol(
            IbftSubProtocol.get(),
            new BftProtocolManager(
                bftEventQueue, peers, IbftSubProtocol.IBFV1, IbftSubProtocol.get().getName()));
  }

  @Override
  protected MiningCoordinator createMiningCoordinator(
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final TransactionPool transactionPool,
      final MiningParameters miningParameters,
      final SyncState syncState,
      final EthProtocolManager ethProtocolManager) {
    final MutableBlockchain blockchain = protocolContext.getBlockchain();
    final BftExecutors bftExecutors = BftExecutors.create(metricsSystem);

    final Address localAddress = Util.publicKeyToAddress(nodeKey.getPublicKey());
    final BftBlockCreatorFactory blockCreatorFactory =
        new BftBlockCreatorFactory(
            gasLimitCalculator,
            transactionPool.getPendingTransactions(),
            protocolContext,
            protocolSchedule,
            miningParameters,
            localAddress,
            bftConfig.getMiningBeneficiary().map(Address::fromHexString).orElse(localAddress),
            bftExtraDataCodec);

    // NOTE: peers should not be used for accessing the network as it does not enforce the
    // "only send once" filter applied by the UniqueMessageMulticaster.
    final VoteTallyCache voteTallyCache =
        protocolContext.getConsensusState(BftContext.class).getVoteTallyCache();

    final ProposerSelector proposerSelector =
        new ProposerSelector(blockchain, blockInterface, true, voteTallyCache);

    peers = new ValidatorPeers(voteTallyCache, IbftSubProtocol.NAME);

    final UniqueMessageMulticaster uniqueMessageMulticaster =
        new UniqueMessageMulticaster(peers, bftConfig.getGossipedHistoryLimit());

    final IbftGossip gossiper = new IbftGossip(uniqueMessageMulticaster);

    final BftFinalState finalState =
        new BftFinalState(
            voteTallyCache,
            nodeKey,
            Util.publicKeyToAddress(nodeKey.getPublicKey()),
            proposerSelector,
            uniqueMessageMulticaster,
            new RoundTimer(bftEventQueue, bftConfig.getRequestTimeoutSeconds(), bftExecutors),
            new BlockTimer(bftEventQueue, bftConfig.getBlockPeriodSeconds(), bftExecutors, clock),
            blockCreatorFactory,
            clock);

    final MessageValidatorFactory messageValidatorFactory =
        new MessageValidatorFactory(
            proposerSelector, protocolSchedule, protocolContext, bftExtraDataCodec);

    final Subscribers<MinedBlockObserver> minedBlockObservers = Subscribers.create();
    minedBlockObservers.subscribe(ethProtocolManager);
    minedBlockObservers.subscribe(blockLogger(transactionPool, localAddress));

    final FutureMessageBuffer futureMessageBuffer =
        new FutureMessageBuffer(
            bftConfig.getFutureMessagesMaxDistance(),
            bftConfig.getFutureMessagesLimit(),
            blockchain.getChainHeadBlockNumber());
    final MessageTracker duplicateMessageTracker =
        new MessageTracker(bftConfig.getDuplicateMessageLimit());

    final MessageFactory messageFactory = new MessageFactory(nodeKey);

    final BftEventHandler ibftController =
        new IbftController(
            blockchain,
            finalState,
            new IbftBlockHeightManagerFactory(
                finalState,
                new IbftRoundFactory(
                    finalState,
                    protocolContext,
                    protocolSchedule,
                    minedBlockObservers,
                    messageValidatorFactory,
                    messageFactory,
                    bftExtraDataCodec),
                messageValidatorFactory,
                messageFactory),
            gossiper,
            duplicateMessageTracker,
            futureMessageBuffer,
            new EthSynchronizerUpdater(ethProtocolManager.ethContext().getEthPeers()));

    final EventMultiplexer eventMultiplexer = new EventMultiplexer(ibftController);
    final BftProcessor bftProcessor = new BftProcessor(bftEventQueue, eventMultiplexer);

    final MiningCoordinator ibftMiningCoordinator =
        new BftMiningCoordinator(
            bftExecutors,
            ibftController,
            bftProcessor,
            blockCreatorFactory,
            blockchain,
            bftEventQueue);
    ibftMiningCoordinator.enable();

    return ibftMiningCoordinator;
  }

  @Override
  protected PluginServiceFactory createAdditionalPluginServices(final Blockchain blockchain) {
    return new IbftQueryPluginServiceFactory(blockchain, blockInterface, nodeKey);
  }

  @Override
  protected ProtocolSchedule createProtocolSchedule() {
    return BftProtocolSchedule.create(
        genesisConfig.getConfigOptions(genesisConfigOverrides),
        privacyParameters,
        isRevertReasonEnabled,
        IbftBlockHeaderValidationRulesetFactory::blockHeaderValidator,
        bftExtraDataCodec);
  }

  @Override
  protected void validateContext(final ProtocolContext context) {
    final BlockHeader genesisBlockHeader = context.getBlockchain().getGenesisBlock().getHeader();

    if (blockInterface.validatorsInBlock(genesisBlockHeader).isEmpty()) {
      LOG.warn("Genesis block contains no signers - chain will not progress.");
    }
  }

  @Override
  protected BftContext createConsensusContext(
      final Blockchain blockchain, final WorldStateArchive worldStateArchive) {
    final GenesisConfigOptions configOptions =
        genesisConfig.getConfigOptions(genesisConfigOverrides);
    final BftConfigOptions ibftConfig = configOptions.getBftConfigOptions();
    final EpochManager epochManager = new EpochManager(ibftConfig.getEpochLength());

    final Map<Long, List<Address>> ibftValidatorForkMap =
        convertIbftForks(configOptions.getTransitions().getIbftForks());

    return new BftContext(
        new ForkingVoteTallyCache(
            blockchain,
            new VoteTallyUpdater(epochManager, blockInterface),
            epochManager,
            blockInterface,
            new BftValidatorOverrides(ibftValidatorForkMap)),
        new VoteProposer(),
        epochManager,
        blockInterface);
  }

  private Map<Long, List<Address>> convertIbftForks(final List<BftFork> bftForks) {
    final Map<Long, List<Address>> result = new HashMap<>();

    for (final BftFork fork : bftForks) {
      fork.getValidators()
          .map(
              validators ->
                  result.put(
                      fork.getForkBlock(),
                      validators.stream()
                          .map(Address::fromHexString)
                          .collect(Collectors.toList())));
    }

    return result;
  }

  private static MinedBlockObserver blockLogger(
      final TransactionPool transactionPool, final Address localAddress) {
    return block ->
        LOG.info(
            String.format(
                "%s #%,d / %d tx / %d pending / %,d (%01.1f%%) gas / (%s)",
                block.getHeader().getCoinbase().equals(localAddress) ? "Produced" : "Imported",
                block.getHeader().getNumber(),
                block.getBody().getTransactions().size(),
                transactionPool.getPendingTransactions().size(),
                block.getHeader().getGasUsed(),
                (block.getHeader().getGasUsed() * 100.0) / block.getHeader().getGasLimit(),
                block.getHash().toHexString()));
  }
}
