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
package org.enterchain.enter.ethereum.eth.manager.task;

import static java.util.Collections.emptyList;

import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.core.Transaction;
import org.enterchain.enter.ethereum.eth.manager.EthContext;
import org.enterchain.enter.ethereum.eth.manager.EthPeer;
import org.enterchain.enter.ethereum.eth.manager.PendingPeerRequest;
import org.enterchain.enter.ethereum.eth.messages.EthPV65;
import org.enterchain.enter.ethereum.eth.messages.PooledTransactionsMessage;
import org.enterchain.enter.ethereum.p2p.rlpx.wire.MessageData;
import org.enterchain.enter.plugin.services.MetricsSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GetPooledTransactionsFromPeerTask extends AbstractPeerRequestTask<List<Transaction>> {

  private static final Logger LOG = LogManager.getLogger();

  private final List<Hash> hashes;

  private GetPooledTransactionsFromPeerTask(
      final EthContext ethContext, final List<Hash> hashes, final MetricsSystem metricsSystem) {
    super(ethContext, EthPV65.GET_POOLED_TRANSACTIONS, metricsSystem);
    this.hashes = new ArrayList<>(hashes);
  }

  public static GetPooledTransactionsFromPeerTask forHashes(
      final EthContext ethContext, final List<Hash> hashes, final MetricsSystem metricsSystem) {
    return new GetPooledTransactionsFromPeerTask(ethContext, hashes, metricsSystem);
  }

  @Override
  protected PendingPeerRequest sendRequest() {
    return sendRequestToPeer(
        peer -> {
          LOG.debug("Requesting {} transaction pool entries from peer {}.", hashes.size(), peer);
          return peer.getPooledTransactions(hashes);
        },
        0);
  }

  @Override
  protected Optional<List<Transaction>> processResponse(
      final boolean streamClosed, final MessageData message, final EthPeer peer) {
    if (streamClosed) {
      // We don't record this as a useless response because it's impossible to know if a peer has
      // the data we're requesting.
      return Optional.of(emptyList());
    }
    final PooledTransactionsMessage pooledTransactionsMessage =
        PooledTransactionsMessage.readFrom(message);
    final List<Transaction> tx = pooledTransactionsMessage.transactions();
    if (tx.size() > hashes.size()) {
      // Can't be the response to our request
      return Optional.empty();
    }
    return mapNodeDataByHash(tx);
  }

  private Optional<List<Transaction>> mapNodeDataByHash(final List<Transaction> transactions) {
    final List<Transaction> result = new ArrayList<>();
    for (final Transaction tx : transactions) {
      final Hash hash = tx.getHash();
      if (!hashes.contains(hash)) {
        return Optional.empty();
      }
      result.add(tx);
    }
    return Optional.of(result);
  }
}