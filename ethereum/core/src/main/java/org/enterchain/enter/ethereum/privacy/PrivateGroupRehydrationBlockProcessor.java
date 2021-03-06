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
package org.enterchain.enter.ethereum.privacy;

import static org.enterchain.enter.ethereum.privacy.PrivateStateRootResolver.EMPTY_ROOT_HASH;

import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.EvmAccount;
import org.enterchain.enter.ethereum.core.Hash;
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
import org.enterchain.enter.ethereum.mainnet.TransactionValidationParams;
import org.enterchain.enter.ethereum.privacy.group.OnChainGroupManagement;
import org.enterchain.enter.ethereum.privacy.storage.PrivateMetadataUpdater;
import org.enterchain.enter.ethereum.privacy.storage.PrivateStateStorage;
import org.enterchain.enter.ethereum.privacy.storage.PrivateTransactionMetadata;
import org.enterchain.enter.ethereum.processing.TransactionProcessingResult;
import org.enterchain.enter.ethereum.vm.BlockHashLookup;
import org.enterchain.enter.ethereum.vm.OperationTracer;
import org.enterchain.enter.ethereum.worldstate.WorldStateArchive;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;

public class PrivateGroupRehydrationBlockProcessor {

  private static final Logger LOG = LogManager.getLogger();

  static final int MAX_GENERATION = 6;

  private final MainnetTransactionProcessor transactionProcessor;
  private final PrivateTransactionProcessor privateTransactionProcessor;
  private final AbstractBlockProcessor.TransactionReceiptFactory transactionReceiptFactory;
  final Wei blockReward;
  private final boolean skipZeroBlockRewards;
  private final MiningBeneficiaryCalculator miningBeneficiaryCalculator;

  public PrivateGroupRehydrationBlockProcessor(
      final MainnetTransactionProcessor transactionProcessor,
      final PrivateTransactionProcessor privateTransactionProcessor,
      final AbstractBlockProcessor.TransactionReceiptFactory transactionReceiptFactory,
      final Wei blockReward,
      final MiningBeneficiaryCalculator miningBeneficiaryCalculator,
      final boolean skipZeroBlockRewards) {
    this.transactionProcessor = transactionProcessor;
    this.privateTransactionProcessor = privateTransactionProcessor;
    this.transactionReceiptFactory = transactionReceiptFactory;
    this.blockReward = blockReward;
    this.miningBeneficiaryCalculator = miningBeneficiaryCalculator;
    this.skipZeroBlockRewards = skipZeroBlockRewards;
  }

  public AbstractBlockProcessor.Result processBlock(
      final Blockchain blockchain,
      final MutableWorldState worldState,
      final WorldStateArchive privateWorldStateArchive,
      final PrivateStateStorage privateStateStorage,
      final PrivateStateRootResolver privateStateRootResolver,
      final Block block,
      final Map<Hash, PrivateTransaction> forExecution,
      final List<BlockHeader> ommers) {
    long gasUsed = 0;
    final List<TransactionReceipt> receipts = new ArrayList<>();

    final List<Transaction> transactions = block.getBody().getTransactions();
    final BlockHeader blockHeader = block.getHeader();
    final PrivateMetadataUpdater metadataUpdater =
        new PrivateMetadataUpdater(blockHeader, privateStateStorage);

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

      final Hash transactionHash = transaction.getHash();
      if (forExecution.containsKey(transactionHash)) {
        final PrivateTransaction privateTransaction = forExecution.get(transactionHash);
        final Bytes32 privacyGroupId = Bytes32.wrap(privateTransaction.getPrivacyGroupId().get());
        final Hash lastRootHash =
            privateStateRootResolver.resolveLastStateRoot(privacyGroupId, metadataUpdater);

        final MutableWorldState disposablePrivateState =
            privateWorldStateArchive.getMutable(lastRootHash, null).get();
        final WorldUpdater privateStateUpdater = disposablePrivateState.updater();
        maybeInjectDefaultManagementAndProxy(
            lastRootHash, disposablePrivateState, privateStateUpdater);
        LOG.debug(
            "Pre-rehydrate root hash: {} for tx {}",
            disposablePrivateState.rootHash(),
            transactionHash);

        final TransactionProcessingResult privateResult =
            privateTransactionProcessor.processTransaction(
                blockchain,
                worldStateUpdater.updater(),
                privateStateUpdater,
                blockHeader,
                transactionHash,
                privateTransaction,
                miningBeneficiary,
                OperationTracer.NO_TRACING,
                new BlockHashLookup(blockHeader, blockchain),
                privateTransaction.getPrivacyGroupId().get());

        privateStateUpdater.commit();
        disposablePrivateState.persist(null);

        storePrivateMetadata(
            transactionHash,
            privacyGroupId,
            disposablePrivateState,
            metadataUpdater,
            privateResult);

        LOG.debug("Post-rehydrate root hash: {}", disposablePrivateState.rootHash());
      }

      // We have to process the public transactions here, because the private transactions can
      // depend on  public state
      final TransactionProcessingResult result =
          transactionProcessor.processTransaction(
              blockchain,
              worldStateUpdater,
              blockHeader,
              transaction,
              miningBeneficiary,
              blockHashLookup,
              false,
              TransactionValidationParams.processingBlock());
      if (result.isInvalid()) {
        return AbstractBlockProcessor.Result.failed();
      }

      gasUsed = transaction.getGasLimit() - result.getGasRemaining() + gasUsed;
      final TransactionReceipt transactionReceipt =
          transactionReceiptFactory.create(transaction.getType(), result, worldState, gasUsed);
      receipts.add(transactionReceipt);
    }

    if (!rewardCoinbase(worldState, blockHeader, ommers, skipZeroBlockRewards)) {
      return AbstractBlockProcessor.Result.failed();
    }

    metadataUpdater.commit();

    return AbstractBlockProcessor.Result.successful(receipts);
  }

  void storePrivateMetadata(
      final Hash commitmentHash,
      final Bytes32 privacyGroupId,
      final MutableWorldState disposablePrivateState,
      final PrivateMetadataUpdater privateMetadataUpdater,
      final TransactionProcessingResult result) {

    final int txStatus =
        result.getStatus() == TransactionProcessingResult.Status.SUCCESSFUL ? 1 : 0;

    final PrivateTransactionReceipt privateTransactionReceipt =
        new PrivateTransactionReceipt(
            txStatus, result.getLogs(), result.getOutput(), result.getRevertReason());

    privateMetadataUpdater.putTransactionReceipt(commitmentHash, privateTransactionReceipt);
    privateMetadataUpdater.updatePrivacyGroupHeadBlockMap(privacyGroupId);
    privateMetadataUpdater.addPrivateTransactionMetadata(
        privacyGroupId,
        new PrivateTransactionMetadata(commitmentHash, disposablePrivateState.rootHash()));
  }

  protected void maybeInjectDefaultManagementAndProxy(
      final Hash lastRootHash,
      final MutableWorldState disposablePrivateState,
      final WorldUpdater privateWorldStateUpdater) {
    if (lastRootHash.equals(EMPTY_ROOT_HASH)) {
      // inject management
      final EvmAccount managementPrecompile =
          privateWorldStateUpdater.createAccount(Address.DEFAULT_ONCHAIN_PRIVACY_MANAGEMENT);
      final MutableAccount mutableManagementPrecompiled = managementPrecompile.getMutable();
      // this is the code for the simple management contract
      mutableManagementPrecompiled.setCode(
          OnChainGroupManagement.DEFAULT_GROUP_MANAGEMENT_RUNTIME_BYTECODE);

      // inject proxy
      final EvmAccount proxyPrecompile =
          privateWorldStateUpdater.createAccount(Address.ONCHAIN_PRIVACY_PROXY);
      final MutableAccount mutableProxyPrecompiled = proxyPrecompile.getMutable();
      // this is the code for the proxy contract
      mutableProxyPrecompiled.setCode(OnChainGroupManagement.PROXY_RUNTIME_BYTECODE);
      // manually set the management contract address so the proxy can trust it
      mutableProxyPrecompiled.setStorageValue(
          UInt256.ZERO,
          UInt256.fromBytes(Bytes32.leftPad(Address.DEFAULT_ONCHAIN_PRIVACY_MANAGEMENT)));

      privateWorldStateUpdater.commit();
      disposablePrivateState.persist(null);
    }
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
