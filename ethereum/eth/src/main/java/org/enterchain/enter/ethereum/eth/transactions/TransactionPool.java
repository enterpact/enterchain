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

import static java.util.Collections.singletonList;
import static org.apache.logging.log4j.LogManager.getLogger;
import static org.enterchain.enter.ethereum.transaction.TransactionInvalidReason.CHAIN_HEAD_WORLD_STATE_NOT_AVAILABLE;

import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.chain.BlockAddedEvent;
import org.enterchain.enter.ethereum.chain.BlockAddedObserver;
import org.enterchain.enter.ethereum.chain.MutableBlockchain;
import org.enterchain.enter.ethereum.core.Account;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.core.Transaction;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.core.fees.BaseFee;
import org.enterchain.enter.ethereum.core.fees.EIP1559;
import org.enterchain.enter.ethereum.core.fees.TransactionPriceCalculator;
import org.enterchain.enter.ethereum.eth.EthProtocol;
import org.enterchain.enter.ethereum.eth.manager.EthContext;
import org.enterchain.enter.ethereum.eth.manager.EthPeer;
import org.enterchain.enter.ethereum.eth.sync.state.SyncState;
import org.enterchain.enter.ethereum.eth.transactions.PendingTransactions.TransactionAddedStatus;
import org.enterchain.enter.ethereum.mainnet.MainnetTransactionValidator;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.mainnet.TransactionValidationParams;
import org.enterchain.enter.ethereum.mainnet.ValidationResult;
import org.enterchain.enter.ethereum.transaction.TransactionInvalidReason;
import org.enterchain.enter.metrics.BesuMetricCategory;
import org.enterchain.enter.plugin.services.MetricsSystem;
import org.enterchain.enter.plugin.services.metrics.Counter;
import org.enterchain.enter.plugin.services.metrics.LabelledMetric;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Logger;

/**
 * Maintains the set of pending transactions received from JSON-RPC or other nodes. Transactions are
 * removed automatically when they are included in a block on the canonical chain and re-added if a
 * re-org removes them from the canonical chain again.
 *
 * <p>This class is safe for use across multiple threads.
 */
public class TransactionPool implements BlockAddedObserver {

  private static final Logger LOG = getLogger();

  private static final long SYNC_TOLERANCE = 100L;
  private static final String REMOTE = "remote";
  private static final String LOCAL = "local";
  private final PendingTransactions pendingTransactions;
  private final ProtocolSchedule protocolSchedule;
  private final ProtocolContext protocolContext;
  private final TransactionBatchAddedListener transactionBatchAddedListener;
  private final TransactionBatchAddedListener pendingTransactionBatchAddedListener;
  private final SyncState syncState;
  private final Wei minTransactionGasPrice;
  private final LabelledMetric<Counter> duplicateTransactionCounter;
  private final PeerTransactionTracker peerTransactionTracker;
  private final PeerPendingTransactionTracker peerPendingTransactionTracker;
  private final Optional<EIP1559> eip1559;
  private final TransactionPriceCalculator frontierPriceCalculator =
      TransactionPriceCalculator.frontier();
  private final TransactionPriceCalculator eip1559PriceCalculator =
      TransactionPriceCalculator.eip1559();
  private final TransactionPoolConfiguration configuration;

  public TransactionPool(
      final PendingTransactions pendingTransactions,
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final TransactionBatchAddedListener transactionBatchAddedListener,
      final TransactionBatchAddedListener pendingTransactionBatchAddedListener,
      final SyncState syncState,
      final EthContext ethContext,
      final PeerTransactionTracker peerTransactionTracker,
      final PeerPendingTransactionTracker peerPendingTransactionTracker,
      final Wei minTransactionGasPrice,
      final MetricsSystem metricsSystem,
      final Optional<EIP1559> eip1559,
      final TransactionPoolConfiguration configuration) {
    this.pendingTransactions = pendingTransactions;
    this.protocolSchedule = protocolSchedule;
    this.protocolContext = protocolContext;
    this.transactionBatchAddedListener = transactionBatchAddedListener;
    this.pendingTransactionBatchAddedListener = pendingTransactionBatchAddedListener;
    this.syncState = syncState;
    this.peerTransactionTracker = peerTransactionTracker;
    this.peerPendingTransactionTracker = peerPendingTransactionTracker;
    this.minTransactionGasPrice = minTransactionGasPrice;
    this.eip1559 = eip1559;
    this.configuration = configuration;

    duplicateTransactionCounter =
        metricsSystem.createLabelledCounter(
            BesuMetricCategory.TRANSACTION_POOL,
            "transactions_duplicates_total",
            "Total number of duplicate transactions received",
            "source");

    ethContext.getEthPeers().subscribeConnect(this::handleConnect);
  }

  void handleConnect(final EthPeer peer) {
    pendingTransactions
        .getLocalTransactions()
        .forEach(transaction -> peerTransactionTracker.addToPeerSendQueue(peer, transaction));

    if (peerPendingTransactionTracker.isPeerSupported(peer, EthProtocol.ETH65)) {
      pendingTransactions
          .getNewPooledHashes()
          .forEach(hash -> peerPendingTransactionTracker.addToPeerSendQueue(peer, hash));
    }
  }

  public boolean addTransactionHash(final Hash transactionHash) {
    return pendingTransactions.addTransactionHash(transactionHash);
  }

  public ValidationResult<TransactionInvalidReason> addLocalTransaction(
      final Transaction transaction) {
    if (!configuration.getTxFeeCap().isZero()
        && minTransactionGasPrice(transaction).compareTo(configuration.getTxFeeCap()) > 0) {
      return ValidationResult.invalid(TransactionInvalidReason.TX_FEECAP_EXCEEDED);
    }
    final ValidationResult<TransactionInvalidReason> validationResult =
        validateTransaction(transaction);
    if (validationResult.isValid()) {
      final TransactionAddedStatus transactionAddedStatus =
          pendingTransactions.addLocalTransaction(transaction);
      if (!transactionAddedStatus.equals(TransactionAddedStatus.ADDED)) {
        duplicateTransactionCounter.labels(LOCAL).inc();
        return ValidationResult.invalid(transactionAddedStatus.getInvalidReason().orElseThrow());
      }
      final Collection<Transaction> txs = singletonList(transaction);
      transactionBatchAddedListener.onTransactionsAdded(txs);
      pendingTransactionBatchAddedListener.onTransactionsAdded(txs);
    }

    return validationResult;
  }

  public void addRemoteTransactions(final Collection<Transaction> transactions) {
    if (!syncState.isInSync(SYNC_TOLERANCE)) {
      return;
    }
    final Set<Transaction> addedTransactions = new HashSet<>();
    for (final Transaction transaction : transactions) {
      pendingTransactions.tryEvictTransactionHash(transaction.getHash());
      if (pendingTransactions.containsTransaction(transaction.getHash())) {
        // We already have this transaction, don't even validate it.
        duplicateTransactionCounter.labels(REMOTE).inc();
        continue;
      }
      final Wei transactionGasPrice = minTransactionGasPrice(transaction);
      if (transactionGasPrice.compareTo(minTransactionGasPrice) < 0) {
        continue;
      }
      final ValidationResult<TransactionInvalidReason> validationResult =
          validateTransaction(transaction);
      if (validationResult.isValid()) {
        final boolean added = pendingTransactions.addRemoteTransaction(transaction);
        if (added) {
          addedTransactions.add(transaction);
        } else {
          duplicateTransactionCounter.labels(REMOTE).inc();
        }
      } else {
        LOG.trace(
            "Validation failed ({}) for transaction {}. Discarding.",
            validationResult.getInvalidReason(),
            transaction);
      }
    }
    if (!addedTransactions.isEmpty()) {
      transactionBatchAddedListener.onTransactionsAdded(addedTransactions);
    }
  }

  public long subscribePendingTransactions(final PendingTransactionListener listener) {
    return pendingTransactions.subscribePendingTransactions(listener);
  }

  public void unsubscribePendingTransactions(final long id) {
    pendingTransactions.unsubscribePendingTransactions(id);
  }

  public long subscribeDroppedTransactions(final PendingTransactionDroppedListener listener) {
    return pendingTransactions.subscribeDroppedTransactions(listener);
  }

  public void unsubscribeDroppedTransactions(final long id) {
    pendingTransactions.unsubscribeDroppedTransactions(id);
  }

  @Override
  public void onBlockAdded(final BlockAddedEvent event) {
    event.getAddedTransactions().forEach(pendingTransactions::transactionAddedToBlock);
    event.getBlock().getHeader().getBaseFee().ifPresent(pendingTransactions::updateBaseFee);
    addRemoteTransactions(event.getRemovedTransactions());
  }

  private MainnetTransactionValidator getTransactionValidator() {
    return protocolSchedule
        .getByBlockNumber(protocolContext.getBlockchain().getChainHeadBlockNumber())
        .getTransactionValidator();
  }

  public PendingTransactions getPendingTransactions() {
    return pendingTransactions;
  }

  private ValidationResult<TransactionInvalidReason> validateTransaction(
      final Transaction transaction) {
    final BlockHeader chainHeadBlockHeader = getChainHeadBlockHeader();
    final ValidationResult<TransactionInvalidReason> basicValidationResult =
        getTransactionValidator().validate(transaction, chainHeadBlockHeader.getBaseFee());
    if (!basicValidationResult.isValid()) {
      return basicValidationResult;
    }

    if (transaction.getGasLimit() > chainHeadBlockHeader.getGasLimit()) {
      return ValidationResult.invalid(
          TransactionInvalidReason.EXCEEDS_BLOCK_GAS_LIMIT,
          String.format(
              "Transaction gas limit of %s exceeds block gas limit of %s",
              transaction.getGasLimit(), chainHeadBlockHeader.getGasLimit()));
    }

    return protocolContext
        .getWorldStateArchive()
        .getMutable(chainHeadBlockHeader.getStateRoot(), chainHeadBlockHeader.getHash(), false)
        .map(
            worldState -> {
              final Account senderAccount = worldState.get(transaction.getSender());
              return getTransactionValidator()
                  .validateForSender(
                      transaction, senderAccount, TransactionValidationParams.transactionPool());
            })
        .orElseGet(() -> ValidationResult.invalid(CHAIN_HEAD_WORLD_STATE_NOT_AVAILABLE));
  }

  public Optional<Transaction> getTransactionByHash(final Hash hash) {
    return pendingTransactions.getTransactionByHash(hash);
  }

  private BlockHeader getChainHeadBlockHeader() {
    final MutableBlockchain blockchain = protocolContext.getBlockchain();
    return blockchain.getBlockHeader(blockchain.getChainHeadHash()).get();
  }

  public interface TransactionBatchAddedListener {

    void onTransactionsAdded(Iterable<Transaction> transactions);
  }

  private Wei minTransactionGasPrice(final Transaction transaction) {
    // EIP-1559 enablement guard block
    if (this.eip1559.isEmpty()) {
      return frontierPriceCalculator.price(transaction, Optional.empty());
    }

    final BlockHeader chainHeadBlockHeader = getChainHeadBlockHeader();
    // Compute transaction price using EIP-1559 rules if chain head is after fork
    if (this.eip1559.get().isEIP1559(chainHeadBlockHeader.getNumber())) {
      return BaseFee.minTransactionPriceInNextBlock(
          transaction, eip1559PriceCalculator, chainHeadBlockHeader::getBaseFee);
    } else { // Use frontier rules otherwise
      return frontierPriceCalculator.price(transaction, Optional.empty());
    }
  }
}
