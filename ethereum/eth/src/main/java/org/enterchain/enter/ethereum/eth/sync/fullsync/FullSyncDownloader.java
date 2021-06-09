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

import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.eth.manager.EthContext;
import org.enterchain.enter.ethereum.eth.sync.ChainDownloader;
import org.enterchain.enter.ethereum.eth.sync.SynchronizerConfiguration;
import org.enterchain.enter.ethereum.eth.sync.TrailingPeerRequirements;
import org.enterchain.enter.ethereum.eth.sync.state.SyncState;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.plugin.services.MetricsSystem;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FullSyncDownloader {

  private static final Logger LOG = LogManager.getLogger();
  private final ChainDownloader chainDownloader;
  private final SynchronizerConfiguration syncConfig;
  private final ProtocolContext protocolContext;
  private final SyncState syncState;

  public FullSyncDownloader(
      final SynchronizerConfiguration syncConfig,
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final EthContext ethContext,
      final SyncState syncState,
      final MetricsSystem metricsSystem) {
    this.syncConfig = syncConfig;
    this.protocolContext = protocolContext;
    this.syncState = syncState;

    this.chainDownloader =
        FullSyncChainDownloader.create(
            syncConfig, protocolSchedule, protocolContext, ethContext, syncState, metricsSystem);
  }

  public void start() {
    LOG.info("Starting full sync.");
    chainDownloader.start();
  }

  public void stop() {
    chainDownloader.cancel();
  }

  public TrailingPeerRequirements calculateTrailingPeerRequirements() {
    return syncState.isInSync()
        ? TrailingPeerRequirements.UNRESTRICTED
        : new TrailingPeerRequirements(
            protocolContext.getBlockchain().getChainHeadBlockNumber(),
            syncConfig.getMaxTrailingPeers());
  }
}
