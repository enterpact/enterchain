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
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.eth.manager.EthContext;
import org.enterchain.enter.ethereum.eth.manager.EthPeer;
import org.enterchain.enter.ethereum.eth.sync.CheckpointHeaderFetcher;
import org.enterchain.enter.ethereum.eth.sync.CheckpointHeaderValidationStep;
import org.enterchain.enter.ethereum.eth.sync.CheckpointRangeSource;
import org.enterchain.enter.ethereum.eth.sync.DownloadBodiesStep;
import org.enterchain.enter.ethereum.eth.sync.DownloadHeadersStep;
import org.enterchain.enter.ethereum.eth.sync.DownloadPipelineFactory;
import org.enterchain.enter.ethereum.eth.sync.SynchronizerConfiguration;
import org.enterchain.enter.ethereum.eth.sync.ValidationPolicy;
import org.enterchain.enter.ethereum.eth.sync.state.SyncTarget;
import org.enterchain.enter.ethereum.mainnet.HeaderValidationMode;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.metrics.BesuMetricCategory;
import org.enterchain.enter.plugin.services.MetricsSystem;
import org.enterchain.enter.services.pipeline.Pipeline;
import org.enterchain.enter.services.pipeline.PipelineBuilder;

import java.util.Optional;

public class FullSyncDownloadPipelineFactory implements DownloadPipelineFactory {

  private final SynchronizerConfiguration syncConfig;
  private final ProtocolSchedule protocolSchedule;
  private final ProtocolContext protocolContext;
  private final EthContext ethContext;
  private final MetricsSystem metricsSystem;
  private final ValidationPolicy detachedValidationPolicy =
      () -> HeaderValidationMode.DETACHED_ONLY;
  private final BetterSyncTargetEvaluator betterSyncTargetEvaluator;

  public FullSyncDownloadPipelineFactory(
      final SynchronizerConfiguration syncConfig,
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final EthContext ethContext,
      final MetricsSystem metricsSystem) {
    this.syncConfig = syncConfig;
    this.protocolSchedule = protocolSchedule;
    this.protocolContext = protocolContext;
    this.ethContext = ethContext;
    this.metricsSystem = metricsSystem;
    betterSyncTargetEvaluator = new BetterSyncTargetEvaluator(syncConfig, ethContext.getEthPeers());
  }

  @Override
  public Pipeline<?> createDownloadPipelineForSyncTarget(final SyncTarget target) {
    final int downloaderParallelism = syncConfig.getDownloaderParallelism();
    final int headerRequestSize = syncConfig.getDownloaderHeaderRequestSize();
    final int singleHeaderBufferSize = headerRequestSize * downloaderParallelism;
    final CheckpointRangeSource checkpointRangeSource =
        new CheckpointRangeSource(
            new CheckpointHeaderFetcher(
                syncConfig, protocolSchedule, ethContext, Optional.empty(), metricsSystem),
            this::shouldContinueDownloadingFromPeer,
            ethContext.getScheduler(),
            target.peer(),
            target.commonAncestor(),
            syncConfig.getDownloaderCheckpointTimeoutsPermitted());
    final DownloadHeadersStep downloadHeadersStep =
        new DownloadHeadersStep(
            protocolSchedule,
            protocolContext,
            ethContext,
            detachedValidationPolicy,
            headerRequestSize,
            metricsSystem);
    final CheckpointHeaderValidationStep validateHeadersJoinUpStep =
        new CheckpointHeaderValidationStep(
            protocolSchedule, protocolContext, detachedValidationPolicy);
    final DownloadBodiesStep downloadBodiesStep =
        new DownloadBodiesStep(protocolSchedule, ethContext, metricsSystem);
    final ExtractTxSignaturesStep extractTxSignaturesStep = new ExtractTxSignaturesStep();
    final FullImportBlockStep importBlockStep =
        new FullImportBlockStep(protocolSchedule, protocolContext, ethContext);

    return PipelineBuilder.createPipelineFrom(
            "fetchCheckpoints",
            checkpointRangeSource,
            downloaderParallelism,
            metricsSystem.createLabelledCounter(
                BesuMetricCategory.SYNCHRONIZER,
                "chain_download_pipeline_processed_total",
                "Number of entries process by each chain download pipeline stage",
                "step",
                "action"),
            true,
            "fullSync")
        .thenProcessAsyncOrdered("downloadHeaders", downloadHeadersStep, downloaderParallelism)
        .thenFlatMap("validateHeadersJoin", validateHeadersJoinUpStep, singleHeaderBufferSize)
        .inBatches(headerRequestSize)
        .thenProcessAsyncOrdered("downloadBodies", downloadBodiesStep, downloaderParallelism)
        .thenFlatMap("extractTxSignatures", extractTxSignaturesStep, singleHeaderBufferSize)
        .andFinishWith("importBlock", importBlockStep);
  }

  private boolean shouldContinueDownloadingFromPeer(
      final EthPeer peer, final BlockHeader lastCheckpointHeader) {
    final boolean caughtUpToPeer =
        peer.chainState().getEstimatedHeight() <= lastCheckpointHeader.getNumber();
    return !peer.isDisconnected()
        && !caughtUpToPeer
        && !betterSyncTargetEvaluator.shouldSwitchSyncTarget(peer);
  }
}
