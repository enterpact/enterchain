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
package org.enterchain.enter.ethereum.retesteth;

import static org.enterchain.enter.config.JsonUtil.normalizeKeys;

import org.enterchain.enter.config.JsonGenesisConfigOptions;
import org.enterchain.enter.config.JsonUtil;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.processor.BlockReplay;
import org.enterchain.enter.ethereum.api.query.BlockchainQueries;
import org.enterchain.enter.ethereum.blockcreation.IncrementingNonceGenerator;
import org.enterchain.enter.ethereum.chain.DefaultBlockchain;
import org.enterchain.enter.ethereum.chain.GenesisState;
import org.enterchain.enter.ethereum.chain.MutableBlockchain;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.BlockHeaderFunctions;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.core.MutableWorldState;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.core.fees.EIP1559;
import org.enterchain.enter.ethereum.eth.manager.EthContext;
import org.enterchain.enter.ethereum.eth.manager.EthMessages;
import org.enterchain.enter.ethereum.eth.manager.EthPeers;
import org.enterchain.enter.ethereum.eth.manager.EthScheduler;
import org.enterchain.enter.ethereum.eth.sync.state.SyncState;
import org.enterchain.enter.ethereum.eth.transactions.ImmutableTransactionPoolConfiguration;
import org.enterchain.enter.ethereum.eth.transactions.PendingTransactions;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPool;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPoolFactory;
import org.enterchain.enter.ethereum.mainnet.EpochCalculator;
import org.enterchain.enter.ethereum.mainnet.HeaderValidationMode;
import org.enterchain.enter.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.enterchain.enter.ethereum.mainnet.MainnetProtocolSchedule;
import org.enterchain.enter.ethereum.mainnet.PoWHasher;
import org.enterchain.enter.ethereum.mainnet.PoWSolution;
import org.enterchain.enter.ethereum.mainnet.PoWSolver;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.mainnet.ProtocolSpec;
import org.enterchain.enter.ethereum.mainnet.ScheduleBasedBlockHeaderFunctions;
import org.enterchain.enter.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage;
import org.enterchain.enter.ethereum.storage.keyvalue.WorldStateKeyValueStorage;
import org.enterchain.enter.ethereum.storage.keyvalue.WorldStatePreimageKeyValueStorage;
import org.enterchain.enter.ethereum.worldstate.DefaultWorldStateArchive;
import org.enterchain.enter.ethereum.worldstate.WorldStateArchive;
import org.enterchain.enter.metrics.noop.NoOpMetricsSystem;
import org.enterchain.enter.plugin.services.MetricsSystem;
import org.enterchain.enter.services.kvstore.InMemoryKeyValueStorage;
import org.enterchain.enter.util.Subscribers;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;

public class RetestethContext {

  private static final Logger LOG = LogManager.getLogger();
  private static final PoWHasher NO_WORK_HASHER =
      (final long nonce, final long number, EpochCalculator epochCalc, final Bytes headerHash) ->
          new PoWSolution(nonce, Hash.ZERO, UInt256.ZERO.toBytes(), Hash.ZERO);

  private final ReentrantLock contextLock = new ReentrantLock();
  private Address coinbase;
  private Bytes extraData;
  private MutableBlockchain blockchain;
  private ProtocolContext protocolContext;
  private BlockchainQueries blockchainQueries;
  private ProtocolSchedule protocolSchedule;
  private BlockHeaderFunctions blockHeaderFunctions;
  private HeaderValidationMode headerValidationMode;
  private BlockReplay blockReplay;
  private RetestethClock retestethClock;

  private TransactionPool transactionPool;
  private EthScheduler ethScheduler;
  private PoWSolver poWSolver;

  public boolean resetContext(
      final String genesisConfigString, final String sealEngine, final Optional<Long> clockTime) {
    contextLock.lock();
    try {
      tearDownContext();
      return buildContext(genesisConfigString, sealEngine, clockTime);
    } catch (final Exception e) {
      LOG.error("Error shutting down existing runner", e);
      return false;
    } finally {
      contextLock.unlock();
    }
  }

  private void tearDownContext() {
    try {
      if (ethScheduler != null) {
        ethScheduler.stop();
        ethScheduler.awaitStop();
      }
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean buildContext(
      final String genesisConfigString, final String sealEngine, final Optional<Long> clockTime) {
    final ObjectNode genesisConfig =
        normalizeKeys(JsonUtil.objectNodeFromString(genesisConfigString));

    retestethClock = new RetestethClock();
    clockTime.ifPresent(retestethClock::resetTime);
    final MetricsSystem metricsSystem = new NoOpMetricsSystem();

    final JsonGenesisConfigOptions jsonGenesisConfigOptions =
        JsonGenesisConfigOptions.fromJsonObject(
            JsonUtil.getObjectNode(genesisConfig, "config").get());
    protocolSchedule = MainnetProtocolSchedule.fromConfig(jsonGenesisConfigOptions);
    if ("NoReward".equalsIgnoreCase(sealEngine)) {
      protocolSchedule = new NoRewardProtocolScheduleWrapper(protocolSchedule);
    }
    blockHeaderFunctions = ScheduleBasedBlockHeaderFunctions.create(protocolSchedule);

    final GenesisState genesisState = GenesisState.fromJson(genesisConfigString, protocolSchedule);
    coinbase = genesisState.getBlock().getHeader().getCoinbase();
    extraData = genesisState.getBlock().getHeader().getExtraData();

    final WorldStateArchive worldStateArchive =
        new DefaultWorldStateArchive(
            new WorldStateKeyValueStorage(new InMemoryKeyValueStorage()),
            new WorldStatePreimageKeyValueStorage(new InMemoryKeyValueStorage()));
    final MutableWorldState worldState = worldStateArchive.getMutable();
    genesisState.writeStateTo(worldState);

    blockchain = createInMemoryBlockchain(genesisState.getBlock());
    protocolContext = new ProtocolContext(blockchain, worldStateArchive, null);

    blockchainQueries = new BlockchainQueries(blockchain, worldStateArchive, ethScheduler);

    final String sealengine = JsonUtil.getString(genesisConfig, "sealengine", "");
    headerValidationMode =
        "NoProof".equals(sealengine) || "NoReward".equals(sealEngine)
            ? HeaderValidationMode.LIGHT
            : HeaderValidationMode.FULL;

    final Iterable<Long> nonceGenerator = new IncrementingNonceGenerator(0);
    poWSolver =
        ("NoProof".equals(sealengine) || "NoReward".equals(sealEngine))
            ? new PoWSolver(
                nonceGenerator,
                NO_WORK_HASHER,
                false,
                Subscribers.none(),
                new EpochCalculator.DefaultEpochCalculator())
            : new PoWSolver(
                nonceGenerator,
                PoWHasher.ETHASH_LIGHT,
                false,
                Subscribers.none(),
                new EpochCalculator.DefaultEpochCalculator());

    blockReplay =
        new BlockReplay(
            protocolSchedule,
            blockchainQueries.getBlockchain(),
            blockchainQueries.getWorldStateArchive());

    // mining support

    final EthPeers ethPeers = new EthPeers("reteseth", retestethClock, metricsSystem);
    final SyncState syncState = new SyncState(blockchain, ethPeers);

    ethScheduler = new EthScheduler(1, 1, 1, 1, metricsSystem);
    final EthContext ethContext = new EthContext(ethPeers, new EthMessages(), ethScheduler);

    final TransactionPoolConfiguration transactionPoolConfiguration =
        ImmutableTransactionPoolConfiguration.builder().build();

    transactionPool =
        TransactionPoolFactory.createTransactionPool(
            protocolSchedule,
            protocolContext,
            ethContext,
            retestethClock,
            metricsSystem,
            syncState,
            Wei.ZERO,
            transactionPoolConfiguration,
            jsonGenesisConfigOptions.getEIP1559BlockNumber().isPresent()
                ? Optional.of(
                    new EIP1559(jsonGenesisConfigOptions.getEIP1559BlockNumber().getAsLong()))
                : Optional.empty());

    LOG.trace("Genesis Block {} ", genesisState::getBlock);

    return true;
  }

  private static MutableBlockchain createInMemoryBlockchain(final Block genesisBlock) {
    return createInMemoryBlockchain(genesisBlock, new MainnetBlockHeaderFunctions());
  }

  private static MutableBlockchain createInMemoryBlockchain(
      final Block genesisBlock, final BlockHeaderFunctions blockHeaderFunctions) {
    final InMemoryKeyValueStorage keyValueStorage = new InMemoryKeyValueStorage();
    return DefaultBlockchain.createMutable(
        genesisBlock,
        new KeyValueStoragePrefixedKeyBlockchainStorage(keyValueStorage, blockHeaderFunctions),
        new NoOpMetricsSystem(),
        100);
  }

  public ProtocolSchedule getProtocolSchedule() {
    return protocolSchedule;
  }

  public BlockHeaderFunctions getBlockHeaderFunctions() {
    return blockHeaderFunctions;
  }

  public ProtocolContext getProtocolContext() {
    return protocolContext;
  }

  public long getBlockHeight() {
    return blockchain.getChainHeadBlockNumber();
  }

  public ProtocolSpec getProtocolSpec(final long blockNumber) {
    return getProtocolSchedule().getByBlockNumber(blockNumber);
  }

  public BlockHeader getBlockHeader(final long blockNumber) {
    return blockchain.getBlockHeader(blockNumber).get();
  }

  public BlockchainQueries getBlockchainQueries() {
    return blockchainQueries;
  }

  public HeaderValidationMode getHeaderValidationMode() {
    return headerValidationMode;
  }

  BlockReplay getBlockReplay() {
    return blockReplay;
  }

  public TransactionPool getTransactionPool() {
    return transactionPool;
  }

  PendingTransactions getPendingTransactions() {
    return transactionPool.getPendingTransactions();
  }

  public Address getCoinbase() {
    return coinbase;
  }

  public Bytes getExtraData() {
    return extraData;
  }

  public MutableBlockchain getBlockchain() {
    return blockchain;
  }

  public RetestethClock getRetestethClock() {
    return retestethClock;
  }

  public PoWSolver getEthHashSolver() {
    return poWSolver;
  }
}
