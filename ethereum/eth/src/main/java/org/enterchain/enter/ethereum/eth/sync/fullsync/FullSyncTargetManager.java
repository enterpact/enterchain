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
package org.enterchain.enter.ethereum.eth.sync.fullsync;

import static java.util.concurrent.CompletableFuture.completedFuture;

import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.chain.MutableBlockchain;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.eth.manager.EthContext;
import org.enterchain.enter.ethereum.eth.manager.EthPeer;
import org.enterchain.enter.ethereum.eth.sync.SyncTargetManager;
import org.enterchain.enter.ethereum.eth.sync.SynchronizerConfiguration;
import org.enterchain.enter.ethereum.eth.sync.state.SyncTarget;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.p2p.rlpx.wire.messages.DisconnectMessage.DisconnectReason;
import org.enterchain.enter.plugin.services.MetricsSystem;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class FullSyncTargetManager extends SyncTargetManager {

  private static final Logger LOG = LogManager.getLogger();
  private final ProtocolContext protocolContext;
  private final EthContext ethContext;

  FullSyncTargetManager(
      final SynchronizerConfiguration config,
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final EthContext ethContext,
      final MetricsSystem metricsSystem) {
    super(config, protocolSchedule, protocolContext, ethContext, metricsSystem);
    this.protocolContext = protocolContext;
    this.ethContext = ethContext;
  }

  @Override
  protected Optional<SyncTarget> finalizeSelectedSyncTarget(final SyncTarget syncTarget) {
    final BlockHeader commonAncestor = syncTarget.commonAncestor();
    if (protocolContext
        .getWorldStateArchive()
        .isWorldStateAvailable(commonAncestor.getStateRoot(), commonAncestor.getHash())) {
      return Optional.of(syncTarget);
    } else {
      LOG.warn(
          "Disconnecting {} because world state is not available at common ancestor at block {} ({})",
          syncTarget.peer(),
          commonAncestor.getNumber(),
          commonAncestor.getHash());
      syncTarget.peer().disconnect(DisconnectReason.USELESS_PEER);
      return Optional.empty();
    }
  }

  @Override
  protected CompletableFuture<Optional<EthPeer>> selectBestAvailableSyncTarget() {
    final Optional<EthPeer> maybeBestPeer = ethContext.getEthPeers().bestPeerWithHeightEstimate();
    if (!maybeBestPeer.isPresent()) {
      LOG.info("No sync target, waiting for peers: {}", ethContext.getEthPeers().peerCount());
      return completedFuture(Optional.empty());
    } else {
      final EthPeer bestPeer = maybeBestPeer.get();
      if (isSyncTargetReached(bestPeer)) {
        // We're caught up to our best peer, try again when a new peer connects
        LOG.debug(
            "Caught up to best peer: {}, Peers: {}",
            bestPeer.chainState().getEstimatedHeight(),
            ethContext.getEthPeers().peerCount());
        return completedFuture(Optional.empty());
      }
      return completedFuture(maybeBestPeer);
    }
  }

  private boolean isSyncTargetReached(final EthPeer peer) {
    final MutableBlockchain blockchain = protocolContext.getBlockchain();
    // We're in sync if the peer's chain is no better than our chain
    return !peer.chainState().chainIsBetterThan(blockchain.getChainHead());
  }

  @Override
  public boolean shouldContinueDownloading() {
    return true;
  }
}
