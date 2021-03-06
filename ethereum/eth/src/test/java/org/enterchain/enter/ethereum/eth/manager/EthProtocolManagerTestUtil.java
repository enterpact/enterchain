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
package org.enterchain.enter.ethereum.eth.manager;

import static com.google.common.base.Preconditions.checkArgument;
import static org.enterchain.enter.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryBlockchain;
import static org.mockito.Mockito.mock;

import org.enterchain.enter.config.GenesisConfigFile;
import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.chain.ChainHead;
import org.enterchain.enter.ethereum.chain.GenesisState;
import org.enterchain.enter.ethereum.core.BlockchainSetupUtil;
import org.enterchain.enter.ethereum.core.Difficulty;
import org.enterchain.enter.ethereum.core.ProtocolScheduleFixture;
import org.enterchain.enter.ethereum.eth.EthProtocol;
import org.enterchain.enter.ethereum.eth.EthProtocolConfiguration;
import org.enterchain.enter.ethereum.eth.manager.DeterministicEthScheduler.TimeoutPolicy;
import org.enterchain.enter.ethereum.eth.peervalidation.PeerValidator;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPool;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.p2p.rlpx.wire.DefaultMessage;
import org.enterchain.enter.ethereum.p2p.rlpx.wire.MessageData;
import org.enterchain.enter.ethereum.worldstate.DataStorageFormat;
import org.enterchain.enter.ethereum.worldstate.WorldStateArchive;
import org.enterchain.enter.metrics.noop.NoOpMetricsSystem;
import org.enterchain.enter.testutil.TestClock;

import java.math.BigInteger;
import java.util.Collections;
import java.util.OptionalLong;

public class EthProtocolManagerTestUtil {

  public static EthProtocolManager create(
      final Blockchain blockchain,
      final TimeoutPolicy timeoutPolicy,
      final WorldStateArchive worldStateArchive,
      final TransactionPool transactionPool,
      final EthProtocolConfiguration ethereumWireProtocolConfiguration) {
    return create(
        blockchain,
        new DeterministicEthScheduler(timeoutPolicy),
        worldStateArchive,
        transactionPool,
        ethereumWireProtocolConfiguration);
  }

  public static EthProtocolManager create(
      final Blockchain blockchain,
      final EthScheduler ethScheduler,
      final WorldStateArchive worldStateArchive,
      final TransactionPool transactionPool,
      final EthProtocolConfiguration ethereumWireProtocolConfiguration,
      final EthPeers ethPeers,
      final EthMessages ethMessages,
      final EthContext ethContext) {
    final BigInteger networkId = BigInteger.ONE;
    return new EthProtocolManager(
        blockchain,
        networkId,
        worldStateArchive,
        transactionPool,
        ethereumWireProtocolConfiguration,
        ethPeers,
        ethMessages,
        ethContext,
        Collections.emptyList(),
        false,
        ethScheduler,
        new ForkIdManager(blockchain, Collections.emptyList(), false));
  }

  public static EthProtocolManager create(final Blockchain blockchain) {
    return create(blockchain, new DeterministicEthScheduler(TimeoutPolicy.NEVER_TIMEOUT));
  }

  public static EthProtocolManager create(
      final Blockchain blockchain,
      final WorldStateArchive worldStateArchive,
      final TransactionPool transactionPool,
      final EthProtocolConfiguration ethProtocolConfiguration) {
    return create(
        blockchain,
        new DeterministicEthScheduler(TimeoutPolicy.NEVER_TIMEOUT),
        worldStateArchive,
        transactionPool,
        ethProtocolConfiguration);
  }

  public static EthProtocolManager create(final EthScheduler ethScheduler) {
    final ProtocolSchedule protocolSchedule = ProtocolScheduleFixture.MAINNET;
    final GenesisConfigFile config = GenesisConfigFile.mainnet();
    final GenesisState genesisState = GenesisState.fromConfig(config, protocolSchedule);
    final Blockchain blockchain = createInMemoryBlockchain(genesisState.getBlock());
    return create(blockchain, ethScheduler);
  }

  public static EthProtocolManager create(
      final Blockchain blockchain,
      final EthScheduler ethScheduler,
      final WorldStateArchive worldStateArchive,
      final TransactionPool transactionPool,
      final EthProtocolConfiguration configuration) {
    EthPeers peers = new EthPeers(EthProtocol.NAME, TestClock.fixed(), new NoOpMetricsSystem());
    EthMessages messages = new EthMessages();

    return create(
        blockchain,
        ethScheduler,
        worldStateArchive,
        transactionPool,
        configuration,
        peers,
        messages,
        new EthContext(peers, messages, ethScheduler));
  }

  public static EthProtocolManager create(
      final Blockchain blockchain, final EthScheduler ethScheduler) {
    EthPeers peers = new EthPeers(EthProtocol.NAME, TestClock.fixed(), new NoOpMetricsSystem());
    EthMessages messages = new EthMessages();

    return create(
        blockchain,
        ethScheduler,
        BlockchainSetupUtil.forTesting(DataStorageFormat.FOREST).getWorldArchive(),
        mock(TransactionPool.class),
        EthProtocolConfiguration.defaultConfig(),
        peers,
        messages,
        new EthContext(peers, messages, ethScheduler));
  }

  public static EthProtocolManager create() {
    return create(TimeoutPolicy.NEVER_TIMEOUT);
  }

  public static EthProtocolManager create(final TimeoutPolicy timeoutPolicy) {
    return create(new DeterministicEthScheduler(timeoutPolicy));
  }

  // Utility to prevent scheduler from automatically running submitted tasks
  public static void disableEthSchedulerAutoRun(final EthProtocolManager ethProtocolManager) {
    final EthScheduler scheduler = ethProtocolManager.ethContext().getScheduler();
    checkArgument(
        scheduler instanceof DeterministicEthScheduler,
        "EthProtocolManager must be set up with "
            + DeterministicEthScheduler.class.getSimpleName()
            + " in order to disable auto run.");
    ((DeterministicEthScheduler) scheduler).disableAutoRun();
  }

  // Manually runs any pending tasks submitted to the EthScheduler
  // Works with {@code disableEthSchedulerAutoRun} - tasks will only be pending if
  // autoRun has been disabled.
  public static void runPendingFutures(final EthProtocolManager ethProtocolManager) {
    final EthScheduler scheduler = ethProtocolManager.ethContext().getScheduler();
    checkArgument(
        scheduler instanceof DeterministicEthScheduler,
        "EthProtocolManager must be set up with "
            + DeterministicEthScheduler.class.getSimpleName()
            + " in order to manually run pending futures.");
    ((DeterministicEthScheduler) scheduler).runPendingFutures();
  }

  /**
   * Expires any pending timeouts tracked by {@code DeterministicEthScheduler}
   *
   * @param ethProtocolManager The {@code EthProtocolManager} managing the scheduler holding the
   *     timeouts to be expired.
   */
  public static void expirePendingTimeouts(final EthProtocolManager ethProtocolManager) {
    final EthScheduler scheduler = ethProtocolManager.ethContext().getScheduler();
    checkArgument(
        scheduler instanceof DeterministicEthScheduler,
        "EthProtocolManager must be set up with "
            + DeterministicEthScheduler.class.getSimpleName()
            + " in order to manually expire pending timeouts.");
    ((DeterministicEthScheduler) scheduler).expirePendingTimeouts();
  }

  /**
   * Gets the number of pending tasks submitted to the EthScheduler.
   *
   * <p>Works with {@code disableEthSchedulerAutoRun} - tasks will only be pending if autoRun has
   * been disabled.
   */
  public static long getPendingFuturesCount(final EthProtocolManager ethProtocolManager) {
    final EthScheduler scheduler = ethProtocolManager.ethContext().getScheduler();
    checkArgument(
        scheduler instanceof DeterministicEthScheduler,
        "EthProtocolManager must be set up with "
            + DeterministicEthScheduler.class.getSimpleName()
            + " in order to manually run pending futures.");
    return ((DeterministicEthScheduler) scheduler).getPendingFuturesCount();
  }

  public static void broadcastMessage(
      final EthProtocolManager ethProtocolManager,
      final RespondingEthPeer peer,
      final MessageData message) {
    ethProtocolManager.processMessage(
        EthProtocol.ETH63, new DefaultMessage(peer.getPeerConnection(), message));
  }

  public static RespondingEthPeer.Builder peerBuilder() {
    return RespondingEthPeer.builder();
  }

  public static RespondingEthPeer createPeer(
      final EthProtocolManager ethProtocolManager, final Difficulty td) {
    return RespondingEthPeer.builder()
        .ethProtocolManager(ethProtocolManager)
        .totalDifficulty(td)
        .build();
  }

  public static RespondingEthPeer createPeer(
      final EthProtocolManager ethProtocolManager,
      final Difficulty td,
      final long estimatedHeight) {
    return RespondingEthPeer.builder()
        .ethProtocolManager(ethProtocolManager)
        .totalDifficulty(td)
        .estimatedHeight(estimatedHeight)
        .build();
  }

  public static RespondingEthPeer createPeer(
      final EthProtocolManager ethProtocolManager,
      final Difficulty td,
      final OptionalLong estimatedHeight) {
    return RespondingEthPeer.builder()
        .ethProtocolManager(ethProtocolManager)
        .totalDifficulty(td)
        .estimatedHeight(estimatedHeight)
        .build();
  }

  public static RespondingEthPeer createPeer(
      final EthProtocolManager ethProtocolManager,
      final Difficulty td,
      final OptionalLong estimatedHeight,
      final PeerValidator... validators) {
    return RespondingEthPeer.builder()
        .ethProtocolManager(ethProtocolManager)
        .totalDifficulty(td)
        .estimatedHeight(estimatedHeight)
        .peerValidators(validators)
        .build();
  }

  public static RespondingEthPeer createPeer(final EthProtocolManager ethProtocolManager) {
    return RespondingEthPeer.builder().ethProtocolManager(ethProtocolManager).build();
  }

  public static RespondingEthPeer createPeer(
      final EthProtocolManager ethProtocolManager, final long estimatedHeight) {
    return RespondingEthPeer.builder()
        .ethProtocolManager(ethProtocolManager)
        .estimatedHeight(estimatedHeight)
        .build();
  }

  public static RespondingEthPeer createPeer(
      final EthProtocolManager ethProtocolManager,
      final long estimatedHeight,
      final PeerValidator... validators) {
    return RespondingEthPeer.builder()
        .ethProtocolManager(ethProtocolManager)
        .estimatedHeight(estimatedHeight)
        .peerValidators(validators)
        .build();
  }

  public static RespondingEthPeer createPeer(
      final EthProtocolManager ethProtocolManager, final Blockchain blockchain) {
    final ChainHead head = blockchain.getChainHead();
    return RespondingEthPeer.builder()
        .ethProtocolManager(ethProtocolManager)
        .totalDifficulty(head.getTotalDifficulty())
        .chainHeadHash(head.getHash())
        .estimatedHeight(blockchain.getChainHeadBlockNumber())
        .build();
  }
}
