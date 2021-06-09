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
package org.enterchain.enter.ethereum.eth.sync.fastsync;

import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.eth.manager.EthContext;
import org.enterchain.enter.ethereum.eth.sync.ChainDownloader;
import org.enterchain.enter.ethereum.eth.sync.PipelineChainDownloader;
import org.enterchain.enter.ethereum.eth.sync.SynchronizerConfiguration;
import org.enterchain.enter.ethereum.eth.sync.state.SyncState;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.plugin.services.MetricsSystem;

public class FastSyncChainDownloader {

  private FastSyncChainDownloader() {}

  public static ChainDownloader create(
      final SynchronizerConfiguration config,
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final EthContext ethContext,
      final SyncState syncState,
      final MetricsSystem metricsSystem,
      final BlockHeader pivotBlockHeader) {

    final FastSyncTargetManager syncTargetManager =
        new FastSyncTargetManager(
            config, protocolSchedule, protocolContext, ethContext, metricsSystem, pivotBlockHeader);

    return new PipelineChainDownloader(
        syncState,
        syncTargetManager,
        new FastSyncDownloadPipelineFactory(
            config, protocolSchedule, protocolContext, ethContext, pivotBlockHeader, metricsSystem),
        ethContext.getScheduler(),
        metricsSystem);
  }
}
