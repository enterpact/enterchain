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
package org.enterchain.enter.ethereum.eth.sync.tasks;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toMap;

import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.BlockBody;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.eth.manager.EthContext;
import org.enterchain.enter.ethereum.eth.manager.EthPeer;
import org.enterchain.enter.ethereum.eth.manager.task.AbstractPeerTask.PeerTaskResult;
import org.enterchain.enter.ethereum.eth.manager.task.AbstractRetryingPeerTask;
import org.enterchain.enter.ethereum.eth.manager.task.GetBodiesFromPeerTask;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.plugin.services.MetricsSystem;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Given a set of headers, "completes" them by repeatedly requesting additional data (bodies) needed
 * to create the blocks that correspond to the supplied headers.
 */
public class CompleteBlocksTask extends AbstractRetryingPeerTask<List<Block>> {
  private static final Logger LOG = LogManager.getLogger();

  private static final int MIN_SIZE_INCOMPLETE_LIST = 1;
  private static final int DEFAULT_RETRIES = 3;

  private final EthContext ethContext;
  private final ProtocolSchedule protocolSchedule;

  private final List<BlockHeader> headers;
  private final Map<Long, Block> blocks;
  private final MetricsSystem metricsSystem;

  private CompleteBlocksTask(
      final ProtocolSchedule protocolSchedule,
      final EthContext ethContext,
      final List<BlockHeader> headers,
      final int maxRetries,
      final MetricsSystem metricsSystem) {
    super(ethContext, maxRetries, Collection::isEmpty, metricsSystem);
    checkArgument(headers.size() > 0, "Must supply a non-empty headers list");
    this.protocolSchedule = protocolSchedule;
    this.ethContext = ethContext;
    this.metricsSystem = metricsSystem;

    this.headers = headers;
    this.blocks =
        headers.stream()
            .filter(this::hasEmptyBody)
            .collect(toMap(BlockHeader::getNumber, header -> new Block(header, BlockBody.empty())));
  }

  private boolean hasEmptyBody(final BlockHeader header) {
    return header.getOmmersHash().equals(Hash.EMPTY_LIST_HASH)
        && header.getTransactionsRoot().equals(Hash.EMPTY_TRIE_HASH);
  }

  public static CompleteBlocksTask forHeaders(
      final ProtocolSchedule protocolSchedule,
      final EthContext ethContext,
      final List<BlockHeader> headers,
      final int maxRetries,
      final MetricsSystem metricsSystem) {
    return new CompleteBlocksTask(protocolSchedule, ethContext, headers, maxRetries, metricsSystem);
  }

  public static CompleteBlocksTask forHeaders(
      final ProtocolSchedule protocolSchedule,
      final EthContext ethContext,
      final List<BlockHeader> headers,
      final MetricsSystem metricsSystem) {
    return new CompleteBlocksTask(
        protocolSchedule, ethContext, headers, DEFAULT_RETRIES, metricsSystem);
  }

  @Override
  protected CompletableFuture<List<Block>> executePeerTask(final Optional<EthPeer> assignedPeer) {
    return requestBodies(assignedPeer).thenCompose(this::processBodiesResult);
  }

  private CompletableFuture<List<Block>> requestBodies(final Optional<EthPeer> assignedPeer) {
    final List<BlockHeader> incompleteHeaders = incompleteHeaders();
    if (incompleteHeaders.isEmpty()) {
      return completedFuture(emptyList());
    }
    LOG.debug(
        "Requesting bodies to complete {} blocks, starting with {}.",
        incompleteHeaders.size(),
        incompleteHeaders.get(0).getNumber());
    return executeSubTask(
        () -> {
          final GetBodiesFromPeerTask task =
              GetBodiesFromPeerTask.forHeaders(
                  protocolSchedule, ethContext, incompleteHeaders, metricsSystem);
          assignedPeer.ifPresent(task::assignPeer);
          return task.run().thenApply(PeerTaskResult::getResult);
        });
  }

  private CompletableFuture<List<Block>> processBodiesResult(final List<Block> blocksResult) {
    blocksResult.forEach((block) -> blocks.put(block.getHeader().getNumber(), block));

    if (incompleteHeaders().isEmpty()) {
      result.complete(
          headers.stream().map(h -> blocks.get(h.getNumber())).collect(Collectors.toList()));
    }

    return completedFuture(blocksResult);
  }

  private List<BlockHeader> incompleteHeaders() {
    final List<BlockHeader> collectedHeaders =
        headers.stream()
            .filter(h -> blocks.get(h.getNumber()) == null)
            .collect(Collectors.toList());
    if (!collectedHeaders.isEmpty() && getRetryCount() > 1) {
      final int subSize = (int) Math.ceil((double) collectedHeaders.size() / getRetryCount());
      if (getRetryCount() > getMaxRetries()) {
        return collectedHeaders.subList(0, MIN_SIZE_INCOMPLETE_LIST);
      } else {
        return collectedHeaders.subList(0, subSize);
      }
    }

    return collectedHeaders;
  }
}
