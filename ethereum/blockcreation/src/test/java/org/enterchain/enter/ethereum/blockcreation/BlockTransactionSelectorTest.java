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
package org.enterchain.enter.ethereum.blockcreation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.enterchain.enter.config.GenesisConfigFile;
import org.enterchain.enter.crypto.KeyPair;
import org.enterchain.enter.crypto.SignatureAlgorithmFactory;
import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.AddressHelpers;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.BlockHeaderBuilder;
import org.enterchain.enter.ethereum.core.Difficulty;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.core.InMemoryKeyValueStorageProvider;
import org.enterchain.enter.ethereum.core.MutableWorldState;
import org.enterchain.enter.ethereum.core.ProcessableBlockHeader;
import org.enterchain.enter.ethereum.core.Transaction;
import org.enterchain.enter.ethereum.core.TransactionReceipt;
import org.enterchain.enter.ethereum.core.TransactionTestFixture;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.core.WorldState;
import org.enterchain.enter.ethereum.core.WorldUpdater;
import org.enterchain.enter.ethereum.core.fees.TransactionGasBudgetCalculator;
import org.enterchain.enter.ethereum.core.fees.TransactionPriceCalculator;
import org.enterchain.enter.ethereum.difficulty.fixed.FixedDifficultyProtocolSchedule;
import org.enterchain.enter.ethereum.eth.transactions.PendingTransactions;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.enterchain.enter.ethereum.mainnet.MainnetTransactionProcessor;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.mainnet.ValidationResult;
import org.enterchain.enter.ethereum.processing.TransactionProcessingResult;
import org.enterchain.enter.ethereum.referencetests.ReferenceTestBlockchain;
import org.enterchain.enter.ethereum.transaction.TransactionInvalidReason;
import org.enterchain.enter.metrics.noop.NoOpMetricsSystem;
import org.enterchain.enter.plugin.data.TransactionType;
import org.enterchain.enter.plugin.services.MetricsSystem;
import org.enterchain.enter.testutil.TestClock;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import org.apache.tuweni.bytes.Bytes;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class BlockTransactionSelectorTest {

  private static final KeyPair keyPair = SignatureAlgorithmFactory.getInstance().generateKeyPair();
  private final MetricsSystem metricsSystem = new NoOpMetricsSystem();

  private final Blockchain blockchain = new ReferenceTestBlockchain();
  private final PendingTransactions pendingTransactions =
      new PendingTransactions(
          TransactionPoolConfiguration.DEFAULT_TX_RETENTION_HOURS,
          5,
          5,
          TestClock.fixed(),
          metricsSystem,
          BlockTransactionSelectorTest::mockBlockHeader,
          TransactionPoolConfiguration.DEFAULT_PRICE_BUMP);
  private final MutableWorldState worldState =
      InMemoryKeyValueStorageProvider.createInMemoryWorldState();
  private final MainnetTransactionProcessor transactionProcessor =
      mock(MainnetTransactionProcessor.class);

  private static BlockHeader mockBlockHeader() {
    final BlockHeader blockHeader = mock(BlockHeader.class);
    when(blockHeader.getBaseFee()).thenReturn(Optional.empty());
    return blockHeader;
  }

  private Boolean isCancelled() {
    return false;
  }

  private ProcessableBlockHeader createBlockWithGasLimit(final long gasLimit) {
    return BlockHeaderBuilder.create()
        .parentHash(Hash.EMPTY)
        .coinbase(Address.fromHexString(String.format("%020x", 1)))
        .difficulty(Difficulty.ONE)
        .number(1)
        .gasLimit(gasLimit)
        .timestamp(Instant.now().toEpochMilli())
        .buildProcessableBlockHeader();
  }

  @Test
  public void emptyPendingTransactionsResultsInEmptyVettingResult() {
    final ProtocolSchedule protocolSchedule =
        FixedDifficultyProtocolSchedule.create(GenesisConfigFile.development().getConfigOptions());
    final MainnetTransactionProcessor mainnetTransactionProcessor =
        protocolSchedule.getByBlockNumber(0).getTransactionProcessor();

    // The block should fit 5 transactions only
    final ProcessableBlockHeader blockHeader = createBlockWithGasLimit(5000);

    final Address miningBeneficiary = AddressHelpers.ofValue(1);

    final BlockTransactionSelector selector =
        new BlockTransactionSelector(
            mainnetTransactionProcessor,
            blockchain,
            worldState,
            pendingTransactions,
            blockHeader,
            this::createReceipt,
            Wei.ZERO,
            0.8,
            this::isCancelled,
            miningBeneficiary,
            TransactionPriceCalculator.frontier(),
            TransactionGasBudgetCalculator.frontier(),
            Optional.empty());

    final BlockTransactionSelector.TransactionSelectionResults results =
        selector.buildTransactionListForBlock(blockHeader.getNumber(), blockHeader.getGasLimit());

    assertThat(results.getTransactions().size()).isEqualTo(0);
    assertThat(results.getReceipts().size()).isEqualTo(0);
    assertThat(results.getFrontierCumulativeGasUsed()).isEqualTo(0);
  }

  @Test
  public void failedTransactionsAreIncludedInTheBlock() {
    final Transaction transaction = createTransaction(1);
    pendingTransactions.addRemoteTransaction(transaction);

    when(transactionProcessor.processTransaction(
            any(), any(), any(), eq(transaction), any(), any(), anyBoolean(), any()))
        .thenReturn(
            TransactionProcessingResult.failed(0, 5, ValidationResult.valid(), Optional.empty()));

    // The block should fit 3 transactions only
    final ProcessableBlockHeader blockHeader = createBlockWithGasLimit(5000);

    final Address miningBeneficiary = AddressHelpers.ofValue(1);

    final BlockTransactionSelector selector =
        new BlockTransactionSelector(
            transactionProcessor,
            blockchain,
            worldState,
            pendingTransactions,
            blockHeader,
            this::createReceipt,
            Wei.ZERO,
            0.8,
            this::isCancelled,
            miningBeneficiary,
            TransactionPriceCalculator.frontier(),
            TransactionGasBudgetCalculator.frontier(),
            Optional.empty());

    final BlockTransactionSelector.TransactionSelectionResults results =
        selector.buildTransactionListForBlock(blockHeader.getNumber(), blockHeader.getGasLimit());

    assertThat(results.getTransactions().size()).isEqualTo(1);
    Assertions.assertThat(results.getTransactions()).contains(transaction);
    assertThat(results.getReceipts().size()).isEqualTo(1);
    assertThat(results.getFrontierCumulativeGasUsed()).isEqualTo(95L);
  }

  @Test
  public void invalidTransactionsTransactionProcessingAreSkippedButBlockStillFills() {
    final List<Transaction> transactionsToInject = Lists.newArrayList();
    for (int i = 0; i < 5; i++) {
      final Transaction tx = createTransaction(i);
      transactionsToInject.add(tx);
      pendingTransactions.addRemoteTransaction(tx);
    }

    when(transactionProcessor.processTransaction(
            any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
        .thenReturn(
            TransactionProcessingResult.successful(
                new ArrayList<>(), 0, 0, Bytes.EMPTY, ValidationResult.valid()));
    when(transactionProcessor.processTransaction(
            any(),
            any(),
            any(),
            eq(transactionsToInject.get(1)),
            any(),
            any(),
            anyBoolean(),
            any()))
        .thenReturn(
            TransactionProcessingResult.invalid(
                ValidationResult.invalid(TransactionInvalidReason.NONCE_TOO_LOW)));

    // The block should fit 3 transactions only
    final ProcessableBlockHeader blockHeader = createBlockWithGasLimit(5000);

    final Address miningBeneficiary = AddressHelpers.ofValue(1);

    final BlockTransactionSelector selector =
        new BlockTransactionSelector(
            transactionProcessor,
            blockchain,
            worldState,
            pendingTransactions,
            blockHeader,
            this::createReceipt,
            Wei.ZERO,
            0.8,
            this::isCancelled,
            miningBeneficiary,
            TransactionPriceCalculator.frontier(),
            TransactionGasBudgetCalculator.frontier(),
            Optional.empty());

    final BlockTransactionSelector.TransactionSelectionResults results =
        selector.buildTransactionListForBlock(blockHeader.getNumber(), blockHeader.getGasLimit());

    assertThat(results.getTransactions().size()).isEqualTo(4);
    assertThat(results.getTransactions().contains(transactionsToInject.get(1))).isFalse();
    assertThat(results.getReceipts().size()).isEqualTo(4);
    assertThat(results.getFrontierCumulativeGasUsed()).isEqualTo(400);
  }

  @Test
  public void subsetOfPendingTransactionsIncludedWhenBlockGasLimitHit() {
    final List<Transaction> transactionsToInject = Lists.newArrayList();
    // Transactions are reported in reverse order.
    for (int i = 0; i < 5; i++) {
      final Transaction tx = createTransaction(i);
      transactionsToInject.add(tx);
      pendingTransactions.addRemoteTransaction(tx);
    }

    when(transactionProcessor.processTransaction(
            any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
        .thenReturn(
            TransactionProcessingResult.successful(
                new ArrayList<>(), 0, 0, Bytes.EMPTY, ValidationResult.valid()));

    final ProcessableBlockHeader blockHeader = createBlockWithGasLimit(301);

    final Address miningBeneficiary = AddressHelpers.ofValue(1);

    final BlockTransactionSelector selector =
        new BlockTransactionSelector(
            transactionProcessor,
            blockchain,
            worldState,
            pendingTransactions,
            blockHeader,
            this::createReceipt,
            Wei.ZERO,
            0.8,
            this::isCancelled,
            miningBeneficiary,
            TransactionPriceCalculator.frontier(),
            TransactionGasBudgetCalculator.frontier(),
            Optional.empty());

    final BlockTransactionSelector.TransactionSelectionResults results =
        selector.buildTransactionListForBlock(blockHeader.getNumber(), blockHeader.getGasLimit());

    assertThat(results.getTransactions().size()).isEqualTo(3);

    assertThat(results.getTransactions().containsAll(transactionsToInject.subList(0, 3))).isTrue();
    assertThat(results.getReceipts().size()).isEqualTo(3);
    assertThat(results.getFrontierCumulativeGasUsed()).isEqualTo(300);

    // Ensure receipts have the correct cumulative gas
    Assertions.assertThat(results.getReceipts().get(0).getCumulativeGasUsed()).isEqualTo(100);
    Assertions.assertThat(results.getReceipts().get(1).getCumulativeGasUsed()).isEqualTo(200);
    Assertions.assertThat(results.getReceipts().get(2).getCumulativeGasUsed()).isEqualTo(300);
  }

  @Test
  public void transactionOfferingGasPriceLessThanMinimumIsIdentifiedAndRemovedFromPending() {
    final ProcessableBlockHeader blockHeader = createBlockWithGasLimit(301);

    final Address miningBeneficiary = AddressHelpers.ofValue(1);
    final BlockTransactionSelector selector =
        new BlockTransactionSelector(
            transactionProcessor,
            blockchain,
            worldState,
            pendingTransactions,
            blockHeader,
            this::createReceipt,
            Wei.of(6),
            0.8,
            this::isCancelled,
            miningBeneficiary,
            TransactionPriceCalculator.frontier(),
            TransactionGasBudgetCalculator.frontier(),
            Optional.empty());

    final Transaction tx = createTransaction(1);
    pendingTransactions.addRemoteTransaction(tx);

    final BlockTransactionSelector.TransactionSelectionResults results =
        selector.buildTransactionListForBlock(blockHeader.getNumber(), blockHeader.getGasLimit());

    assertThat(results.getTransactions().size()).isEqualTo(0);
    assertThat(pendingTransactions.size()).isEqualTo(0);
  }

  @Test
  public void transactionTooLargeForBlockDoesNotPreventMoreBeingAddedIfBlockOccupancyNotReached() {
    final ProcessableBlockHeader blockHeader = createBlockWithGasLimit(300);

    when(transactionProcessor.processTransaction(
            any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
        .thenReturn(
            TransactionProcessingResult.successful(
                new ArrayList<>(), 0, 0, Bytes.EMPTY, ValidationResult.valid()));

    final Address miningBeneficiary = AddressHelpers.ofValue(1);
    final BlockTransactionSelector selector =
        new BlockTransactionSelector(
            transactionProcessor,
            blockchain,
            worldState,
            pendingTransactions,
            blockHeader,
            this::createReceipt,
            Wei.ZERO,
            0.8,
            this::isCancelled,
            miningBeneficiary,
            TransactionPriceCalculator.frontier(),
            TransactionGasBudgetCalculator.frontier(),
            Optional.empty());

    final TransactionTestFixture txTestFixture = new TransactionTestFixture();
    // Add 3 transactions to the Pending Transactions, 79% of block, 100% of block and 10% of block
    // should end up selecting the first and third only.
    // NOTE - PendingTransactions outputs these in nonce order
    final List<Transaction> transactionsToInject = Lists.newArrayList();
    transactionsToInject.add(
        txTestFixture
            .gasLimit((long) (blockHeader.getGasLimit() * 0.79))
            .nonce(1)
            .createTransaction(keyPair));
    transactionsToInject.add(
        txTestFixture.gasLimit(blockHeader.getGasLimit()).nonce(2).createTransaction(keyPair));
    transactionsToInject.add(
        txTestFixture
            .gasLimit((long) (blockHeader.getGasLimit() * 0.1))
            .nonce(3)
            .createTransaction(keyPair));

    for (final Transaction tx : transactionsToInject) {
      pendingTransactions.addRemoteTransaction(tx);
    }

    final BlockTransactionSelector.TransactionSelectionResults results =
        selector.buildTransactionListForBlock(blockHeader.getNumber(), blockHeader.getGasLimit());

    assertThat(results.getTransactions().size()).isEqualTo(2);
    Assertions.assertThat(results.getTransactions().get(0)).isEqualTo(transactionsToInject.get(0));
    Assertions.assertThat(results.getTransactions().get(1)).isEqualTo(transactionsToInject.get(2));
  }

  @Test
  public void transactionSelectionStopsWhenSufficientBlockOccupancyIsReached() {
    final ProcessableBlockHeader blockHeader = createBlockWithGasLimit(300);

    // TransactionProcessor mock assumes all gas in the transaction was used (i.e. gasLimit).
    when(transactionProcessor.processTransaction(
            any(), any(), any(), any(), any(), any(), anyBoolean(), any()))
        .thenReturn(
            TransactionProcessingResult.successful(
                new ArrayList<>(), 0, 0, Bytes.EMPTY, ValidationResult.valid()));

    final Address miningBeneficiary = AddressHelpers.ofValue(1);
    final BlockTransactionSelector selector =
        new BlockTransactionSelector(
            transactionProcessor,
            blockchain,
            worldState,
            pendingTransactions,
            blockHeader,
            this::createReceipt,
            Wei.ZERO,
            0.8,
            this::isCancelled,
            miningBeneficiary,
            TransactionPriceCalculator.frontier(),
            TransactionGasBudgetCalculator.frontier(),
            Optional.empty());

    final TransactionTestFixture txTestFixture = new TransactionTestFixture();
    // Add 4 transactions to the Pending Transactions 15% (ok), 79% (ok), 25% (too large), 10%
    // (not included, it would fit, however previous transaction was too large and block was
    // suitably populated).
    // NOTE - PendingTransactions will output these in nonce order.
    final Transaction transaction1 =
        txTestFixture
            .gasLimit((long) (blockHeader.getGasLimit() * 0.15))
            .nonce(1)
            .createTransaction(keyPair);
    final Transaction transaction2 =
        txTestFixture
            .gasLimit((long) (blockHeader.getGasLimit() * 0.79))
            .nonce(2)
            .createTransaction(keyPair);
    final Transaction transaction3 =
        txTestFixture
            .gasLimit((long) (blockHeader.getGasLimit() * 0.25))
            .nonce(3)
            .createTransaction(keyPair);
    final Transaction transaction4 =
        txTestFixture
            .gasLimit((long) (blockHeader.getGasLimit() * 0.1))
            .nonce(4)
            .createTransaction(keyPair);

    pendingTransactions.addRemoteTransaction(transaction1);
    pendingTransactions.addRemoteTransaction(transaction2);
    pendingTransactions.addRemoteTransaction(transaction3);
    pendingTransactions.addRemoteTransaction(transaction4);

    final BlockTransactionSelector.TransactionSelectionResults results =
        selector.buildTransactionListForBlock(blockHeader.getNumber(), blockHeader.getGasLimit());

    assertThat(results.getTransactions().size()).isEqualTo(2);
    Assertions.assertThat(results.getTransactions().get(0)).isEqualTo(transaction1);
    Assertions.assertThat(results.getTransactions().get(1)).isEqualTo(transaction2);
    assertThat(results.getTransactions().contains(transaction4)).isFalse();
    assertThat(results.getTransactions().contains(transaction3)).isFalse();
  }

  @Test
  public void shouldDiscardTransactionsThatFailValidation() {
    final ProcessableBlockHeader blockHeader = createBlockWithGasLimit(300);

    final Address miningBeneficiary = AddressHelpers.ofValue(1);
    final BlockTransactionSelector selector =
        new BlockTransactionSelector(
            transactionProcessor,
            blockchain,
            worldState,
            pendingTransactions,
            blockHeader,
            this::createReceipt,
            Wei.ZERO,
            0.8,
            this::isCancelled,
            miningBeneficiary,
            TransactionPriceCalculator.frontier(),
            TransactionGasBudgetCalculator.frontier(),
            Optional.empty());

    final TransactionTestFixture txTestFixture = new TransactionTestFixture();
    final Transaction validTransaction =
        txTestFixture.nonce(1).gasLimit(1).createTransaction(keyPair);
    final Transaction invalidTransaction =
        txTestFixture.nonce(2).gasLimit(2).createTransaction(keyPair);

    pendingTransactions.addRemoteTransaction(validTransaction);
    pendingTransactions.addRemoteTransaction(invalidTransaction);

    when(transactionProcessor.processTransaction(
            eq(blockchain),
            any(WorldUpdater.class),
            eq(blockHeader),
            eq(validTransaction),
            any(),
            any(),
            anyBoolean(),
            any()))
        .thenReturn(
            TransactionProcessingResult.successful(
                new ArrayList<>(), 2000, 10000, Bytes.EMPTY, ValidationResult.valid()));
    when(transactionProcessor.processTransaction(
            eq(blockchain),
            any(WorldUpdater.class),
            eq(blockHeader),
            eq(invalidTransaction),
            any(),
            any(),
            anyBoolean(),
            any()))
        .thenReturn(
            TransactionProcessingResult.invalid(
                ValidationResult.invalid(TransactionInvalidReason.EXCEEDS_BLOCK_GAS_LIMIT)));

    selector.buildTransactionListForBlock(blockHeader.getNumber(), blockHeader.getGasLimit());

    Assertions.assertThat(pendingTransactions.getTransactionByHash(validTransaction.getHash()))
        .isPresent();
    Assertions.assertThat(pendingTransactions.getTransactionByHash(invalidTransaction.getHash()))
        .isNotPresent();
  }

  @Test
  public void transactionWithIncorrectNonceRemainsInPoolAndNotSelected() {
    final ProcessableBlockHeader blockHeader = createBlockWithGasLimit(5000);

    final TransactionTestFixture txTestFixture = new TransactionTestFixture();
    final Transaction futureTransaction =
        txTestFixture.nonce(5).gasLimit(1).createTransaction(keyPair);

    pendingTransactions.addRemoteTransaction(futureTransaction);

    when(transactionProcessor.processTransaction(
            eq(blockchain),
            any(WorldUpdater.class),
            eq(blockHeader),
            eq(futureTransaction),
            any(),
            any(),
            anyBoolean(),
            any()))
        .thenReturn(
            TransactionProcessingResult.invalid(
                ValidationResult.invalid(TransactionInvalidReason.INCORRECT_NONCE)));

    final Address miningBeneficiary = AddressHelpers.ofValue(1);
    final BlockTransactionSelector selector =
        new BlockTransactionSelector(
            transactionProcessor,
            blockchain,
            worldState,
            pendingTransactions,
            blockHeader,
            this::createReceipt,
            Wei.ZERO,
            0.8,
            this::isCancelled,
            miningBeneficiary,
            TransactionPriceCalculator.frontier(),
            TransactionGasBudgetCalculator.frontier(),
            Optional.empty());

    final BlockTransactionSelector.TransactionSelectionResults results =
        selector.buildTransactionListForBlock(blockHeader.getNumber(), blockHeader.getGasLimit());

    Assertions.assertThat(pendingTransactions.getTransactionByHash(futureTransaction.getHash()))
        .isPresent();
    assertThat(results.getTransactions().size()).isEqualTo(0);
  }

  private Transaction createTransaction(final int transactionNumber) {
    return Transaction.builder()
        .gasLimit(100)
        .gasPrice(Wei.of(5))
        .nonce(transactionNumber)
        .payload(Bytes.EMPTY)
        .to(Address.ID)
        .value(Wei.of(transactionNumber))
        .sender(Address.ID)
        .chainId(BigInteger.ONE)
        .guessType()
        .signAndBuild(keyPair);
  }

  // This is a duplicate of the MainnetProtocolSpec::frontierTransactionReceiptFactory
  private TransactionReceipt createReceipt(
      final TransactionType __,
      final TransactionProcessingResult result,
      final WorldState worldState,
      final long gasUsed) {
    return new TransactionReceipt(
        worldState.rootHash(), gasUsed, Lists.newArrayList(), Optional.empty());
  }
}
