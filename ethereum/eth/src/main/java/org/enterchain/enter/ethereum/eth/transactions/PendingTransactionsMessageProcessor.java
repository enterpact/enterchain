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
package org.enterchain.enter.ethereum.eth.transactions;

import static java.time.Instant.now;
import static org.apache.logging.log4j.LogManager.getLogger;

import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.eth.manager.EthContext;
import org.enterchain.enter.ethereum.eth.manager.EthPeer;
import org.enterchain.enter.ethereum.eth.manager.task.BufferedGetPooledTransactionsFromPeerFetcher;
import org.enterchain.enter.ethereum.eth.messages.NewPooledTransactionHashesMessage;
import org.enterchain.enter.ethereum.eth.sync.state.SyncState;
import org.enterchain.enter.ethereum.p2p.rlpx.wire.messages.DisconnectMessage.DisconnectReason;
import org.enterchain.enter.ethereum.rlp.RLPException;
import org.enterchain.enter.metrics.RunnableCounter;
import org.enterchain.enter.plugin.services.MetricsSystem;
import org.enterchain.enter.plugin.services.metrics.Counter;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;

public class PendingTransactionsMessageProcessor {

  private static final int SKIPPED_MESSAGES_LOGGING_THRESHOLD = 1000;
  private static final long SYNC_TOLERANCE = 100L;

  private static final Logger LOG = getLogger();

  private final ConcurrentHashMap<EthPeer, BufferedGetPooledTransactionsFromPeerFetcher>
      scheduledTasks;

  private final PeerPendingTransactionTracker transactionTracker;
  private final Counter totalSkippedTransactionsMessageCounter;
  private final TransactionPool transactionPool;
  private final TransactionPoolConfiguration transactionPoolConfiguration;
  private final EthContext ethContext;
  private final MetricsSystem metricsSystem;
  private final SyncState syncState;

  public PendingTransactionsMessageProcessor(
      final PeerPendingTransactionTracker transactionTracker,
      final TransactionPool transactionPool,
      final TransactionPoolConfiguration transactionPoolConfiguration,
      final Counter metricsCounter,
      final EthContext ethContext,
      final MetricsSystem metricsSystem,
      final SyncState syncState) {
    this.transactionTracker = transactionTracker;
    this.transactionPool = transactionPool;
    this.transactionPoolConfiguration = transactionPoolConfiguration;
    this.ethContext = ethContext;
    this.metricsSystem = metricsSystem;
    this.syncState = syncState;
    this.totalSkippedTransactionsMessageCounter =
        new RunnableCounter(
            metricsCounter,
            () ->
                LOG.warn(
                    "{} expired transaction messages have been skipped.",
                    SKIPPED_MESSAGES_LOGGING_THRESHOLD),
            SKIPPED_MESSAGES_LOGGING_THRESHOLD);
    this.scheduledTasks = new ConcurrentHashMap<>();
  }

  void processNewPooledTransactionHashesMessage(
      final EthPeer peer,
      final NewPooledTransactionHashesMessage transactionsMessage,
      final Instant startedAt,
      final Duration keepAlive) {
    // Check if message not expired.
    if (startedAt.plus(keepAlive).isAfter(now())) {
      this.processNewPooledTransactionHashesMessage(peer, transactionsMessage);
    } else {
      totalSkippedTransactionsMessageCounter.inc();
    }
  }

  @SuppressWarnings("UnstableApiUsage")
  private void processNewPooledTransactionHashesMessage(
      final EthPeer peer, final NewPooledTransactionHashesMessage transactionsMessage) {
    try {
      LOG.trace("Received pooled transaction hashes message from {}", peer);

      transactionTracker.markTransactionsHashesAsSeen(
          peer, transactionsMessage.pendingTransactions());
      if (syncState.isInSync(SYNC_TOLERANCE)) {
        final BufferedGetPooledTransactionsFromPeerFetcher bufferedTask =
            scheduledTasks.computeIfAbsent(
                peer,
                ethPeer -> {
                  ethContext
                      .getScheduler()
                      .scheduleFutureTask(
                          new FetcherCreatorTask(peer),
                          transactionPoolConfiguration.getEth65TrxAnnouncedBufferingPeriod());
                  return new BufferedGetPooledTransactionsFromPeerFetcher(peer, this);
                });

        for (final Hash hash : transactionsMessage.pendingTransactions()) {
          if (transactionPool.getTransactionByHash(hash).isEmpty()
              && transactionPool.addTransactionHash(hash)) {
            bufferedTask.addHash(hash);
          }
        }
      }
    } catch (final RLPException ex) {
      if (peer != null) {
        LOG.debug(
            "Malformed pooled transaction hashes message received, disconnecting: {}", peer, ex);
        peer.disconnect(DisconnectReason.BREACH_OF_PROTOCOL);
      }
    }
  }

  public TransactionPool getTransactionPool() {
    return transactionPool;
  }

  public EthContext getEthContext() {
    return ethContext;
  }

  public MetricsSystem getMetricsSystem() {
    return metricsSystem;
  }

  public class FetcherCreatorTask implements Runnable {
    final EthPeer peer;

    public FetcherCreatorTask(final EthPeer peer) {
      this.peer = peer;
    }

    @Override
    public void run() {
      if (peer != null) {
        final BufferedGetPooledTransactionsFromPeerFetcher fetcher = scheduledTasks.remove(peer);
        if (!peer.isDisconnected()) {
          fetcher.requestTransactions();
        }
      }
    }
  }
}