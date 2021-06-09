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
package org.enterchain.enter.ethereum.eth.manager.ethtaskutils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.core.BlockchainSetupUtil;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.eth.EthProtocol;
import org.enterchain.enter.ethereum.eth.EthProtocolConfiguration;
import org.enterchain.enter.ethereum.eth.manager.DeterministicEthScheduler;
import org.enterchain.enter.ethereum.eth.manager.EthContext;
import org.enterchain.enter.ethereum.eth.manager.EthMessages;
import org.enterchain.enter.ethereum.eth.manager.EthPeer;
import org.enterchain.enter.ethereum.eth.manager.EthPeers;
import org.enterchain.enter.ethereum.eth.manager.EthProtocolManager;
import org.enterchain.enter.ethereum.eth.manager.EthProtocolManagerTestUtil;
import org.enterchain.enter.ethereum.eth.manager.EthScheduler;
import org.enterchain.enter.ethereum.eth.manager.RespondingEthPeer;
import org.enterchain.enter.ethereum.eth.manager.task.EthTask;
import org.enterchain.enter.ethereum.eth.sync.state.SyncState;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPool;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPoolFactory;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.worldstate.DataStorageFormat;
import org.enterchain.enter.metrics.noop.NoOpMetricsSystem;
import org.enterchain.enter.plugin.services.MetricsSystem;
import org.enterchain.enter.testutil.TestClock;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @param <T> The type of data being requested from the network
 * @param <R> The type of data returned from the network
 */
public abstract class AbstractMessageTaskTest<T, R> {
  protected static Blockchain blockchain;
  protected static ProtocolSchedule protocolSchedule;
  protected static ProtocolContext protocolContext;
  protected static MetricsSystem metricsSystem = new NoOpMetricsSystem();
  protected EthProtocolManager ethProtocolManager;
  protected EthContext ethContext;
  protected EthPeers ethPeers;
  protected TransactionPool transactionPool;
  protected AtomicBoolean peersDoTimeout;
  protected AtomicInteger peerCountToTimeout;

  @BeforeClass
  public static void setup() {
    final BlockchainSetupUtil blockchainSetupUtil =
        BlockchainSetupUtil.forTesting(DataStorageFormat.FOREST);
    blockchainSetupUtil.importAllBlocks();
    blockchain = blockchainSetupUtil.getBlockchain();
    protocolSchedule = blockchainSetupUtil.getProtocolSchedule();
    protocolContext = blockchainSetupUtil.getProtocolContext();

    assertThat(blockchainSetupUtil.getMaxBlockNumber()).isGreaterThanOrEqualTo(20L);
  }

  @Before
  public void setupTest() {
    peersDoTimeout = new AtomicBoolean(false);
    peerCountToTimeout = new AtomicInteger(0);
    ethPeers = spy(new EthPeers(EthProtocol.NAME, TestClock.fixed(), metricsSystem));
    final EthMessages ethMessages = new EthMessages();
    final EthScheduler ethScheduler =
        new DeterministicEthScheduler(
            () -> peerCountToTimeout.getAndDecrement() > 0 || peersDoTimeout.get());
    ethContext = new EthContext(ethPeers, ethMessages, ethScheduler);
    final SyncState syncState = new SyncState(blockchain, ethContext.getEthPeers());
    transactionPool =
        TransactionPoolFactory.createTransactionPool(
            protocolSchedule,
            protocolContext,
            ethContext,
            TestClock.fixed(),
            metricsSystem,
            syncState,
            Wei.of(1),
            TransactionPoolConfiguration.DEFAULT,
            Optional.empty());
    ethProtocolManager =
        EthProtocolManagerTestUtil.create(
            blockchain,
            ethScheduler,
            protocolContext.getWorldStateArchive(),
            transactionPool,
            EthProtocolConfiguration.defaultConfig(),
            ethPeers,
            ethMessages,
            ethContext);
  }

  protected abstract T generateDataToBeRequested();

  protected abstract EthTask<R> createTask(T requestedData);

  protected abstract void assertResultMatchesExpectation(
      T requestedData, R response, EthPeer respondingPeer);

  @Test
  public void completesWhenPeersAreResponsive() {
    // Setup a responsive peer
    final RespondingEthPeer.Responder responder =
        RespondingEthPeer.blockchainResponder(
            blockchain, protocolContext.getWorldStateArchive(), transactionPool);
    final RespondingEthPeer respondingPeer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, 1000);

    // Setup data to be requested and expected response
    final T requestedData = generateDataToBeRequested();

    // Execute task and wait for response
    final AtomicReference<R> actualResult = new AtomicReference<>();
    final AtomicBoolean done = new AtomicBoolean(false);
    final EthTask<R> task = createTask(requestedData);
    final CompletableFuture<R> future = task.run();
    respondingPeer.respondWhile(responder, () -> !future.isDone());
    future.whenComplete(
        (result, error) -> {
          actualResult.set(result);
          done.compareAndSet(false, true);
        });

    assertThat(done).isTrue();
    assertResultMatchesExpectation(requestedData, actualResult.get(), respondingPeer.getEthPeer());
  }

  @Test
  public void doesNotCompleteWhenPeersDoNotRespond() {
    // Setup a unresponsive peer
    EthProtocolManagerTestUtil.createPeer(ethProtocolManager, 1000);

    // Setup data to be requested
    final T requestedData = generateDataToBeRequested();

    // Execute task and wait for response
    final AtomicBoolean done = new AtomicBoolean(false);
    final EthTask<R> task = createTask(requestedData);
    final CompletableFuture<R> future = task.run();
    future.whenComplete(
        (response, error) -> {
          done.compareAndSet(false, true);
        });
    assertThat(done).isFalse();
  }

  @Test
  public void cancel() {
    // Setup a unresponsive peer
    EthProtocolManagerTestUtil.createPeer(ethProtocolManager, 1000);

    // Setup data to be requested
    final T requestedData = generateDataToBeRequested();

    // Execute task
    final EthTask<R> task = createTask(requestedData);
    final CompletableFuture<R> future = task.run();

    assertThat(future.isDone()).isFalse();
    task.cancel();
    assertThat(future.isDone()).isTrue();
    assertThat(future.isCancelled()).isTrue();
    assertThat(task.run().isCancelled()).isTrue();
  }
}
