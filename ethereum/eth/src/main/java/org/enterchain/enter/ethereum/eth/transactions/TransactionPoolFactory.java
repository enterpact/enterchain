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

import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.core.fees.EIP1559;
import org.enterchain.enter.ethereum.eth.manager.EthContext;
import org.enterchain.enter.ethereum.eth.messages.EthPV62;
import org.enterchain.enter.ethereum.eth.messages.EthPV65;
import org.enterchain.enter.ethereum.eth.sync.state.SyncState;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.metrics.BesuMetricCategory;
import org.enterchain.enter.plugin.services.MetricsSystem;

import java.time.Clock;
import java.util.Optional;

public class TransactionPoolFactory {

  public static TransactionPool createTransactionPool(
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final EthContext ethContext,
      final Clock clock,
      final MetricsSystem metricsSystem,
      final SyncState syncState,
      final Wei minTransactionGasPrice,
      final TransactionPoolConfiguration transactionPoolConfiguration,
      final Optional<EIP1559> eip1559) {

    final PendingTransactions pendingTransactions =
        new PendingTransactions(
            transactionPoolConfiguration.getPendingTxRetentionPeriod(),
            transactionPoolConfiguration.getTxPoolMaxSize(),
            transactionPoolConfiguration.getPooledTransactionHashesSize(),
            clock,
            metricsSystem,
            protocolContext.getBlockchain()::getChainHeadHeader,
            transactionPoolConfiguration.getPriceBump());

    final PeerTransactionTracker transactionTracker = new PeerTransactionTracker();
    final TransactionsMessageSender transactionsMessageSender =
        new TransactionsMessageSender(transactionTracker);

    final PeerPendingTransactionTracker pendingTransactionTracker =
        new PeerPendingTransactionTracker(pendingTransactions);
    final PendingTransactionsMessageSender pendingTransactionsMessageSender =
        new PendingTransactionsMessageSender(pendingTransactionTracker);

    return createTransactionPool(
        protocolSchedule,
        protocolContext,
        ethContext,
        metricsSystem,
        syncState,
        minTransactionGasPrice,
        transactionPoolConfiguration,
        pendingTransactions,
        transactionTracker,
        transactionsMessageSender,
        pendingTransactionTracker,
        pendingTransactionsMessageSender,
        eip1559);
  }

  static TransactionPool createTransactionPool(
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final EthContext ethContext,
      final MetricsSystem metricsSystem,
      final SyncState syncState,
      final Wei minTransactionGasPrice,
      final TransactionPoolConfiguration transactionPoolConfiguration,
      final PendingTransactions pendingTransactions,
      final PeerTransactionTracker transactionTracker,
      final TransactionsMessageSender transactionsMessageSender,
      final PeerPendingTransactionTracker pendingTransactionTracker,
      final PendingTransactionsMessageSender pendingTransactionsMessageSender,
      final Optional<EIP1559> eip1559) {
    final TransactionPool transactionPool =
        new TransactionPool(
            pendingTransactions,
            protocolSchedule,
            protocolContext,
            new TransactionSender(transactionTracker, transactionsMessageSender, ethContext),
            new PendingTransactionSender(
                pendingTransactionTracker, pendingTransactionsMessageSender, ethContext),
            syncState,
            ethContext,
            transactionTracker,
            pendingTransactionTracker,
            minTransactionGasPrice,
            metricsSystem,
            eip1559,
            transactionPoolConfiguration);
    final TransactionsMessageHandler transactionsMessageHandler =
        new TransactionsMessageHandler(
            ethContext.getScheduler(),
            new TransactionsMessageProcessor(
                transactionTracker,
                transactionPool,
                metricsSystem.createCounter(
                    BesuMetricCategory.TRANSACTION_POOL,
                    "transactions_messages_skipped_total",
                    "Total number of transactions messages skipped by the processor.")),
            transactionPoolConfiguration.getTxMessageKeepAliveSeconds());
    ethContext.getEthMessages().subscribe(EthPV62.TRANSACTIONS, transactionsMessageHandler);
    final PendingTransactionsMessageHandler pooledTransactionsMessageHandler =
        new PendingTransactionsMessageHandler(
            ethContext.getScheduler(),
            new PendingTransactionsMessageProcessor(
                pendingTransactionTracker,
                transactionPool,
                transactionPoolConfiguration,
                metricsSystem.createCounter(
                    BesuMetricCategory.TRANSACTION_POOL,
                    "pending_transactions_messages_skipped_total",
                    "Total number of pending transactions messages skipped by the processor."),
                ethContext,
                metricsSystem,
                syncState),
            transactionPoolConfiguration.getTxMessageKeepAliveSeconds());
    ethContext
        .getEthMessages()
        .subscribe(EthPV65.NEW_POOLED_TRANSACTION_HASHES, pooledTransactionsMessageHandler);
    ethContext.getEthPeers().subscribeDisconnect(pendingTransactionTracker);

    protocolContext.getBlockchain().observeBlockAdded(transactionPool);
    ethContext.getEthPeers().subscribeDisconnect(transactionTracker);
    return transactionPool;
  }
}
