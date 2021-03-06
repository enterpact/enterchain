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
package org.enterchain.enter.ethereum.api.query;

import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.core.Transaction;
import org.enterchain.enter.ethereum.core.TransactionReceipt;

public class TransactionReceiptWithMetadata {
  private final TransactionReceipt receipt;
  private final Hash transactionHash;
  private final int transactionIndex;
  private final long gasUsed;
  private final long blockNumber;
  private final Hash blockHash;
  private final Transaction transaction;

  private TransactionReceiptWithMetadata(
      final TransactionReceipt receipt,
      final Transaction transaction,
      final Hash transactionHash,
      final int transactionIndex,
      final long gasUsed,
      final Hash blockHash,
      final long blockNumber) {
    this.receipt = receipt;
    this.transactionHash = transactionHash;
    this.transactionIndex = transactionIndex;
    this.gasUsed = gasUsed;
    this.blockHash = blockHash;
    this.blockNumber = blockNumber;
    this.transaction = transaction;
  }

  public static TransactionReceiptWithMetadata create(
      final TransactionReceipt receipt,
      final Transaction transaction,
      final Hash transactionHash,
      final int transactionIndex,
      final long gasUsed,
      final Hash blockHash,
      final long blockNumber) {
    return new TransactionReceiptWithMetadata(
        receipt, transaction, transactionHash, transactionIndex, gasUsed, blockHash, blockNumber);
  }

  public TransactionReceipt getReceipt() {
    return receipt;
  }

  public Hash getTransactionHash() {
    return transactionHash;
  }

  public Transaction getTransaction() {
    return transaction;
  }

  public int getTransactionIndex() {
    return transactionIndex;
  }

  public Hash getBlockHash() {
    return blockHash;
  }

  public long getBlockNumber() {
    return blockNumber;
  }

  // The gas used for this particular transaction (as opposed to cumulativeGas which is included in
  // the receipt itself)
  public long getGasUsed() {
    return gasUsed;
  }
}
