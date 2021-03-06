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
package org.enterchain.enter.ethereum.eth.manager;

import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.core.BlockBody;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.core.Transaction;
import org.enterchain.enter.ethereum.core.TransactionReceipt;
import org.enterchain.enter.ethereum.eth.EthProtocolConfiguration;
import org.enterchain.enter.ethereum.eth.messages.BlockBodiesMessage;
import org.enterchain.enter.ethereum.eth.messages.BlockHeadersMessage;
import org.enterchain.enter.ethereum.eth.messages.EthPV62;
import org.enterchain.enter.ethereum.eth.messages.EthPV63;
import org.enterchain.enter.ethereum.eth.messages.EthPV65;
import org.enterchain.enter.ethereum.eth.messages.GetBlockBodiesMessage;
import org.enterchain.enter.ethereum.eth.messages.GetBlockHeadersMessage;
import org.enterchain.enter.ethereum.eth.messages.GetNodeDataMessage;
import org.enterchain.enter.ethereum.eth.messages.GetPooledTransactionsMessage;
import org.enterchain.enter.ethereum.eth.messages.GetReceiptsMessage;
import org.enterchain.enter.ethereum.eth.messages.NodeDataMessage;
import org.enterchain.enter.ethereum.eth.messages.PooledTransactionsMessage;
import org.enterchain.enter.ethereum.eth.messages.ReceiptsMessage;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPool;
import org.enterchain.enter.ethereum.p2p.rlpx.connections.PeerConnection.PeerNotConnected;
import org.enterchain.enter.ethereum.p2p.rlpx.wire.MessageData;
import org.enterchain.enter.ethereum.p2p.rlpx.wire.messages.DisconnectMessage.DisconnectReason;
import org.enterchain.enter.ethereum.rlp.RLPException;
import org.enterchain.enter.ethereum.worldstate.WorldStateArchive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

class EthServer {
  private static final Logger LOG = LogManager.getLogger();

  private final Blockchain blockchain;
  private final WorldStateArchive worldStateArchive;
  private final TransactionPool transactionPool;
  private final EthMessages ethMessages;
  private final EthProtocolConfiguration ethereumWireProtocolConfiguration;

  EthServer(
      final Blockchain blockchain,
      final WorldStateArchive worldStateArchive,
      final TransactionPool transactionPool,
      final EthMessages ethMessages,
      final EthProtocolConfiguration ethereumWireProtocolConfiguration) {
    this.blockchain = blockchain;
    this.worldStateArchive = worldStateArchive;
    this.transactionPool = transactionPool;
    this.ethMessages = ethMessages;
    this.ethereumWireProtocolConfiguration = ethereumWireProtocolConfiguration;
    this.setupListeners();
  }

  private void setupListeners() {
    ethMessages.subscribe(EthPV62.GET_BLOCK_HEADERS, this::handleGetBlockHeaders);
    ethMessages.subscribe(EthPV62.GET_BLOCK_BODIES, this::handleGetBlockBodies);
    ethMessages.subscribe(EthPV63.GET_RECEIPTS, this::handleGetReceipts);
    ethMessages.subscribe(EthPV63.GET_NODE_DATA, this::handleGetNodeData);
    ethMessages.subscribe(EthPV65.GET_POOLED_TRANSACTIONS, this::handleGetPooledTransactions);
  }

  private void handleGetBlockHeaders(final EthMessage message) {
    LOG.trace("Responding to GET_BLOCK_HEADERS request");
    try {
      final MessageData response =
          constructGetHeadersResponse(
              blockchain,
              message.getData(),
              ethereumWireProtocolConfiguration.getMaxGetBlockHeaders());
      message.getPeer().send(response);
    } catch (final RLPException e) {
      LOG.debug(
          "Received malformed GET_BLOCK_HEADERS message, disconnecting: {}", message.getPeer(), e);
      message.getPeer().disconnect(DisconnectReason.BREACH_OF_PROTOCOL);
    } catch (final PeerNotConnected peerNotConnected) {
      // Peer disconnected before we could respond - nothing to do
    }
  }

  private void handleGetBlockBodies(final EthMessage message) {
    LOG.trace("Responding to GET_BLOCK_BODIES request");
    try {
      final MessageData response =
          constructGetBodiesResponse(
              blockchain,
              message.getData(),
              ethereumWireProtocolConfiguration.getMaxGetBlockBodies());
      message.getPeer().send(response);
    } catch (final RLPException e) {
      LOG.debug(
          "Received malformed GET_BLOCK_BODIES message, disconnecting: {}", message.getPeer(), e);
      message.getPeer().disconnect(DisconnectReason.BREACH_OF_PROTOCOL);
    } catch (final PeerNotConnected peerNotConnected) {
      // Peer disconnected before we could respond - nothing to do
    }
  }

  private void handleGetReceipts(final EthMessage message) {
    LOG.trace("Responding to GET_RECEIPTS request");
    try {
      final MessageData response =
          constructGetReceiptsResponse(
              blockchain, message.getData(), ethereumWireProtocolConfiguration.getMaxGetReceipts());
      message.getPeer().send(response);
    } catch (final RLPException e) {
      LOG.debug("Received malformed GET_RECEIPTS message, disconnecting: {}", message.getPeer(), e);
      message.getPeer().disconnect(DisconnectReason.BREACH_OF_PROTOCOL);
    } catch (final PeerNotConnected peerNotConnected) {
      // Peer disconnected before we could respond - nothing to do
    }
  }

  private void handleGetNodeData(final EthMessage message) {
    LOG.trace("Responding to GET_NODE_DATA request");
    try {
      final MessageData response =
          constructGetNodeDataResponse(
              worldStateArchive,
              message.getData(),
              ethereumWireProtocolConfiguration.getMaxGetNodeData());
      message.getPeer().send(response);
    } catch (final RLPException e) {
      LOG.debug(
          "Received malformed GET_NODE_DATA message, disconnecting: {}", message.getPeer(), e);
      message.getPeer().disconnect(DisconnectReason.BREACH_OF_PROTOCOL);
    } catch (final PeerNotConnected peerNotConnected) {
      // Peer disconnected before we could respond - nothing to do
    }
  }

  private void handleGetPooledTransactions(final EthMessage message) {
    LOG.trace("Responding to GET_POOLED_TRANSACTIONS request");
    try {
      final MessageData response =
          constructGetPooledTransactionsResponse(
              transactionPool,
              message.getData(),
              ethereumWireProtocolConfiguration.getMaxGetPooledTransactions());
      message.getPeer().send(response);
    } catch (final RLPException e) {
      LOG.debug(
          "Received malformed GET_POOLED_TRANSACTIONS message, disconnecting: {}",
          message.getPeer(),
          e);
      message.getPeer().disconnect(DisconnectReason.BREACH_OF_PROTOCOL);
    } catch (final PeerNotConnected peerNotConnected) {
      // Peer disconnected before we could respond - nothing to do
    }
  }

  static MessageData constructGetHeadersResponse(
      final Blockchain blockchain, final MessageData message, final int requestLimit) {
    final GetBlockHeadersMessage getHeaders = GetBlockHeadersMessage.readFrom(message);
    final Optional<Hash> hash = getHeaders.hash();
    final int skip = getHeaders.skip();
    final int maxHeaders = Math.min(requestLimit, getHeaders.maxHeaders());
    final boolean reversed = getHeaders.reverse();
    final BlockHeader firstHeader;
    if (hash.isPresent()) {
      final Hash startHash = hash.get();
      firstHeader = blockchain.getBlockHeader(startHash).orElse(null);
    } else {
      final long firstNumber = getHeaders.blockNumber().getAsLong();
      firstHeader = blockchain.getBlockHeader(firstNumber).orElse(null);
    }
    final Collection<BlockHeader> resp;
    if (firstHeader == null) {
      resp = Collections.emptyList();
    } else {
      resp = Lists.newArrayList(firstHeader);
      final long numberDelta = reversed ? -(skip + 1) : (skip + 1);
      for (int i = 1; i < maxHeaders; i++) {
        final long blockNumber = firstHeader.getNumber() + i * numberDelta;
        if (blockNumber < BlockHeader.GENESIS_BLOCK_NUMBER) {
          break;
        }
        final Optional<BlockHeader> maybeHeader = blockchain.getBlockHeader(blockNumber);
        if (maybeHeader.isPresent()) {
          resp.add(maybeHeader.get());
        } else {
          break;
        }
      }
    }
    return BlockHeadersMessage.create(resp);
  }

  static MessageData constructGetBodiesResponse(
      final Blockchain blockchain, final MessageData message, final int requestLimit) {
    final GetBlockBodiesMessage getBlockBodiesMessage = GetBlockBodiesMessage.readFrom(message);
    final Iterable<Hash> hashes = getBlockBodiesMessage.hashes();

    final Collection<BlockBody> bodies = new ArrayList<>();
    int count = 0;
    for (final Hash hash : hashes) {
      if (count >= requestLimit) {
        break;
      }
      count++;
      final Optional<BlockBody> maybeBody = blockchain.getBlockBody(hash);
      if (!maybeBody.isPresent()) {
        continue;
      }
      bodies.add(maybeBody.get());
    }
    return BlockBodiesMessage.create(bodies);
  }

  static MessageData constructGetReceiptsResponse(
      final Blockchain blockchain, final MessageData message, final int requestLimit) {
    final GetReceiptsMessage getReceipts = GetReceiptsMessage.readFrom(message);
    final Iterable<Hash> hashes = getReceipts.hashes();

    final List<List<TransactionReceipt>> receipts = new ArrayList<>();
    int count = 0;
    for (final Hash hash : hashes) {
      if (count >= requestLimit) {
        break;
      }
      count++;
      final Optional<List<TransactionReceipt>> maybeReceipts = blockchain.getTxReceipts(hash);
      if (!maybeReceipts.isPresent()) {
        continue;
      }
      receipts.add(maybeReceipts.get());
    }
    return ReceiptsMessage.create(receipts);
  }

  static MessageData constructGetPooledTransactionsResponse(
      final TransactionPool transactionPool, final MessageData message, final int requestLimit) {
    final GetPooledTransactionsMessage getPooledTransactions =
        GetPooledTransactionsMessage.readFrom(message);
    final Iterable<Hash> hashes = getPooledTransactions.pooledTransactions();

    final List<Transaction> tx = new ArrayList<>();
    int count = 0;
    for (final Hash hash : hashes) {
      if (count >= requestLimit) {
        break;
      }
      count++;
      final Optional<Transaction> maybeTx = transactionPool.getTransactionByHash(hash);
      if (maybeTx.isEmpty()) {
        continue;
      }
      tx.add(maybeTx.get());
    }
    return PooledTransactionsMessage.create(tx);
  }

  static MessageData constructGetNodeDataResponse(
      final WorldStateArchive worldStateArchive,
      final MessageData message,
      final int requestLimit) {
    final GetNodeDataMessage getNodeDataMessage = GetNodeDataMessage.readFrom(message);
    final Iterable<Hash> hashes = getNodeDataMessage.hashes();

    final List<Bytes> nodeData = new ArrayList<>();
    int count = 0;
    for (final Hash hash : hashes) {
      if (count >= requestLimit) {
        break;
      }
      count++;

      worldStateArchive.getNodeData(hash).ifPresent(nodeData::add);
    }
    return NodeDataMessage.create(nodeData);
  }
}
