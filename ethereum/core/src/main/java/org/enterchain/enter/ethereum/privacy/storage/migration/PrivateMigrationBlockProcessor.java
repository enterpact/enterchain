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
package org.enterchain.enter.ethereum.privacy.storage.migration;

import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.MutableAccount;
import org.enterchain.enter.ethereum.core.MutableWorldState;
import org.enterchain.enter.ethereum.core.ProcessableBlockHeader;
import org.enterchain.enter.ethereum.core.Transaction;
import org.enterchain.enter.ethereum.core.TransactionReceipt;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.core.WorldUpdater;
import org.enterchain.enter.ethereum.mainnet.AbstractBlockProcessor;
import org.enterchain.enter.ethereum.mainnet.MainnetTransactionProcessor;
import org.enterchain.enter.ethereum.mainnet.MiningBeneficiaryCalculator;
import org.enterchain.enter.ethereum.mainnet.ProtocolSpec;
import org.enterchain.enter.ethereum.mainnet.TransactionValidationParams;
import org.enterchain.enter.ethereum.processing.TransactionProcessingResult;
import org.enterchain.enter.ethereum.vm.BlockHashLookup;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PrivateMigrationBlockProcessor {

  private static final Logger LOG = LogManager.getLogger();

  static final int MAX_GENERATION = 6;

  private final MainnetTransactionProcessor transactionProcessor;
  private final AbstractBlockProcessor.TransactionReceiptFactory transactionReceiptFactory;
  final Wei blockReward;
  private final boolean skipZeroBlockRewards;
  private final MiningBeneficiaryCalculator miningBeneficiaryCalculator;

  public PrivateMigrationBlockProcessor(
      final MainnetTransactionProcessor transactionProcessor,
      final AbstractBlockProcessor.TransactionReceiptFactory transactionReceiptFactory,
      final Wei blockReward,
      final MiningBeneficiaryCalculator miningBeneficiaryCalculator,
      final boolean skipZeroBlockRewards) {
    this.transactionProcessor = transactionProcessor;
    this.transactionReceiptFactory = transactionReceiptFactory;
    this.blockReward = blockReward;
    this.miningBeneficiaryCalculator = miningBeneficiaryCalculator;
    this.skipZeroBlockRewards = skipZeroBlockRewards;
  }

  public PrivateMigrationBlockProcessor(final ProtocolSpec protocolSpec) {
    this(
        protocolSpec.getTransactionProcessor(),
        protocolSpec.getTransactionReceiptFactory(),
        protocolSpec.getBlockReward(),
        protocolSpec.getMiningBeneficiaryCalculator(),
        protocolSpec.isSkipZeroBlockRewards());
  }

  public AbstractBlockProcessor.Result processBlock(
      final Blockchain blockchain,
      final MutableWorldState worldState,
      final BlockHeader blockHeader,
      final List<Transaction> transactions,
      final List<BlockHeader> ommers) {
    long gasUsed = 0;
    final List<TransactionReceipt> receipts = new ArrayList<>();

    for (final Transaction transaction : transactions) {
      final long remainingGasBudget = blockHeader.getGasLimit() - gasUsed;
      if (Long.compareUnsigned(transaction.getGasLimit(), remainingGasBudget) > 0) {
        LOG.warn(
            "Transaction processing error: transaction gas limit {} exceeds available block budget"
                + " remaining {}",
            transaction.getGasLimit(),
            remainingGasBudget);
        return AbstractBlockProcessor.Result.failed();
      }

      final WorldUpdater worldStateUpdater = worldState.updater();
      final BlockHashLookup blockHashLookup = new BlockHashLookup(blockHeader, blockchain);
      final Address miningBeneficiary =
          miningBeneficiaryCalculator.calculateBeneficiary(blockHeader);

      final TransactionProcessingResult result =
          transactionProcessor.processTransaction(
              blockchain,
              worldStateUpdater,
              blockHeader,
              transaction,
              miningBeneficiary,
              blockHashLookup,
              true,
              TransactionValidationParams.processingBlock());
      if (result.isInvalid()) {
        return AbstractBlockProcessor.Result.failed();
      }

      worldStateUpdater.commit();
      gasUsed = transaction.getGasLimit() - result.getGasRemaining() + gasUsed;
      final TransactionReceipt transactionReceipt =
          transactionReceiptFactory.create(transaction.getType(), result, worldState, gasUsed);
      receipts.add(transactionReceipt);
    }

    if (!rewardCoinbase(worldState, blockHeader, ommers, skipZeroBlockRewards)) {
      return AbstractBlockProcessor.Result.failed();
    }

    return AbstractBlockProcessor.Result.successful(receipts);
  }

  private boolean rewardCoinbase(
      final MutableWorldState worldState,
      final ProcessableBlockHeader header,
      final List<BlockHeader> ommers,
      final boolean skipZeroBlockRewards) {
    if (skipZeroBlockRewards && blockReward.isZero()) {
      return true;
    }

    final Wei coinbaseReward = blockReward.add(blockReward.multiply(ommers.size()).divide(32));
    final WorldUpdater updater = worldState.updater();
    final MutableAccount coinbase = updater.getOrCreate(header.getCoinbase()).getMutable();

    coinbase.incrementBalance(coinbaseReward);
    for (final BlockHeader ommerHeader : ommers) {
      if (ommerHeader.getNumber() - header.getNumber() > MAX_GENERATION) {
        LOG.warn(
            "Block processing error: ommer block number {} more than {} generations current block"
                + " number {}",
            ommerHeader.getNumber(),
            MAX_GENERATION,
            header.getNumber());
        return false;
      }

      final MutableAccount ommerCoinbase =
          updater.getOrCreate(ommerHeader.getCoinbase()).getMutable();
      final long distance = header.getNumber() - ommerHeader.getNumber();
      final Wei ommerReward = blockReward.subtract(blockReward.multiply(distance).divide(8));
      ommerCoinbase.incrementBalance(ommerReward);
    }

    return true;
  }
}
