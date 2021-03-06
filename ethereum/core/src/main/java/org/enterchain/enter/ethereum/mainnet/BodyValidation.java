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
package org.enterchain.enter.ethereum.mainnet;

import static org.enterchain.enter.crypto.Hash.keccak256;

import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.core.LogsBloomFilter;
import org.enterchain.enter.ethereum.core.Transaction;
import org.enterchain.enter.ethereum.core.TransactionReceipt;
import org.enterchain.enter.ethereum.core.encoding.TransactionEncoder;
import org.enterchain.enter.ethereum.rlp.RLP;
import org.enterchain.enter.ethereum.trie.MerklePatriciaTrie;
import org.enterchain.enter.ethereum.trie.SimpleMerklePatriciaTrie;

import java.util.List;
import java.util.stream.IntStream;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;

/** A utility class for body validation tasks. */
public final class BodyValidation {

  private BodyValidation() {
    // Utility Class
  }

  private static Bytes indexKey(final int i) {
    return RLP.encodeOne(UInt256.valueOf(i).toBytes().trimLeadingZeros());
  }

  private static MerklePatriciaTrie<Bytes, Bytes> trie() {
    return new SimpleMerklePatriciaTrie<>(b -> b);
  }

  /**
   * Generates the transaction root for a list of transactions
   *
   * @param transactions the transactions
   * @return the transaction root
   */
  public static Hash transactionsRoot(final List<Transaction> transactions) {
    final MerklePatriciaTrie<Bytes, Bytes> trie = trie();

    IntStream.range(0, transactions.size())
        .forEach(
            i -> trie.put(indexKey(i), TransactionEncoder.encodeOpaqueBytes(transactions.get(i))));

    return Hash.wrap(trie.getRootHash());
  }

  /**
   * Generates the receipt root for a list of receipts
   *
   * @param receipts the receipts
   * @return the receipt root
   */
  public static Hash receiptsRoot(final List<TransactionReceipt> receipts) {
    final MerklePatriciaTrie<Bytes, Bytes> trie = trie();

    IntStream.range(0, receipts.size())
        .forEach(
            i ->
                trie.put(
                    indexKey(i),
                    RLP.encode(
                        rlpOutput -> receipts.get(i).writeToForReceiptTrie(rlpOutput, false))));

    return Hash.wrap(trie.getRootHash());
  }

  /**
   * Generates the ommers hash for a list of ommer block headers
   *
   * @param ommers the ommer block headers
   * @return the ommers hash
   */
  public static Hash ommersHash(final List<BlockHeader> ommers) {
    return Hash.wrap(keccak256(RLP.encode(out -> out.writeList(ommers, BlockHeader::writeTo))));
  }

  /**
   * Generates the logs bloom filter for a list of transaction receipts
   *
   * @param receipts the transaction receipts
   * @return the logs bloom filter
   */
  public static LogsBloomFilter logsBloom(final List<TransactionReceipt> receipts) {
    final LogsBloomFilter.Builder filterBuilder = LogsBloomFilter.builder();

    receipts.forEach(receipt -> filterBuilder.insertFilter(receipt.getBloomFilter()));

    return filterBuilder.build();
  }
}
