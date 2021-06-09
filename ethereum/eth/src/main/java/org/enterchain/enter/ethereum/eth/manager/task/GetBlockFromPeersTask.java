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

import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.eth.manager.EthContext;
import org.enterchain.enter.ethereum.eth.manager.EthPeer;
import org.enterchain.enter.ethereum.eth.manager.exceptions.IncompleteResultsException;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.plugin.services.MetricsSystem;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Downloads a block from a peer. Will complete exceptionally if block cannot be downloaded. */
public class GetBlockFromPeersTask extends AbstractEthTask<AbstractPeerTask.PeerTaskResult<Block>> {
  private static final Logger LOG = LogManager.getLogger();

  private final List<EthPeer> peers;
  private final EthContext ethContext;
  private final ProtocolSchedule protocolSchedule;
  private final Hash hash;
  private final long blockNumber;
  private final MetricsSystem metricsSystem;

  protected GetBlockFromPeersTask(
      final List<EthPeer> peers,
      final ProtocolSchedule protocolSchedule,
      final EthContext ethContext,
      final Hash hash,
      final long blockNumber,
      final MetricsSystem metricsSystem) {
    super(metricsSystem);
    this.peers = peers;
    this.ethContext = ethContext;
    this.blockNumber = blockNumber;
    this.metricsSystem = metricsSystem;
    this.protocolSchedule = protocolSchedule;
    this.hash = hash;
  }

  public static GetBlockFromPeersTask create(
      final List<EthPeer> peers,
      final ProtocolSchedule protocolSchedule,
      final EthContext ethContext,
      final Hash hash,
      final long blockNumber,
      final MetricsSystem metricsSystem) {
    return new GetBlockFromPeersTask(
        peers, protocolSchedule, ethContext, hash, blockNumber, metricsSystem);
  }

  @Override
  protected void executeTask() {
    LOG.debug("Downloading block {} from peers {}.", hash, peers.stream().map(EthPeer::toString));
    getBlockFromPeers(peers);
  }

  private void getBlockFromPeers(final List<EthPeer> peers) {
    if (peers.isEmpty()) {
      result.completeExceptionally(new IncompleteResultsException());
    }
    final EthPeer peer = peers.get(0);
    if (peer.isDisconnected()) {
      getBlockFromPeers(peers.subList(1, peers.size()));
    }
    LOG.debug("Trying downloading block {} from peer {}.", hash, peer);
    final AbstractPeerTask<Block> getBlockTask =
        GetBlockFromPeerTask.create(protocolSchedule, ethContext, hash, blockNumber, metricsSystem)
            .assignPeer(peer);
    getBlockTask
        .run()
        .whenComplete(
            (r, t) -> {
              if (t != null) {
                getBlockFromPeers(peers.subList(1, peers.size()));
              } else {
                result.complete(r);
              }
            });
  }
}
