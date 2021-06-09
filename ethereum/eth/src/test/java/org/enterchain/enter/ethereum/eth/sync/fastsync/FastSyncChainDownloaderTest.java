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

import static org.assertj.core.api.Assertions.assertThat;
import static org.enterchain.enter.ethereum.mainnet.HeaderValidationMode.LIGHT_SKIP_DETACHED;
import static org.enterchain.enter.ethereum.p2p.rlpx.wire.messages.DisconnectMessage.DisconnectReason.TOO_MANY_PEERS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.chain.MutableBlockchain;
import org.enterchain.enter.ethereum.core.BlockchainSetupUtil;
import org.enterchain.enter.ethereum.eth.manager.EthContext;
import org.enterchain.enter.ethereum.eth.manager.EthProtocolManager;
import org.enterchain.enter.ethereum.eth.manager.EthProtocolManagerTestUtil;
import org.enterchain.enter.ethereum.eth.manager.EthScheduler;
import org.enterchain.enter.ethereum.eth.manager.RespondingEthPeer;
import org.enterchain.enter.ethereum.eth.messages.EthPV62;
import org.enterchain.enter.ethereum.eth.messages.GetBlockHeadersMessage;
import org.enterchain.enter.ethereum.eth.sync.ChainDownloader;
import org.enterchain.enter.ethereum.eth.sync.SynchronizerConfiguration;
import org.enterchain.enter.ethereum.eth.sync.state.SyncState;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.worldstate.DataStorageFormat;
import org.enterchain.enter.metrics.noop.NoOpMetricsSystem;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.LockSupport;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class FastSyncChainDownloaderTest {

  private final FastSyncValidationPolicy validationPolicy = mock(FastSyncValidationPolicy.class);

  protected ProtocolSchedule protocolSchedule;
  protected EthProtocolManager ethProtocolManager;
  protected EthContext ethContext;
  protected ProtocolContext protocolContext;
  private SyncState syncState;

  protected MutableBlockchain localBlockchain;
  private BlockchainSetupUtil otherBlockchainSetup;
  protected Blockchain otherBlockchain;

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{DataStorageFormat.BONSAI}, {DataStorageFormat.FOREST}});
  }

  private final DataStorageFormat storageFormat;

  public FastSyncChainDownloaderTest(final DataStorageFormat storageFormat) {
    this.storageFormat = storageFormat;
  }

  @Before
  public void setup() {
    when(validationPolicy.getValidationModeForNextBlock()).thenReturn(LIGHT_SKIP_DETACHED);
    final BlockchainSetupUtil localBlockchainSetup = BlockchainSetupUtil.forTesting(storageFormat);
    localBlockchain = localBlockchainSetup.getBlockchain();
    otherBlockchainSetup = BlockchainSetupUtil.forTesting(storageFormat);
    otherBlockchain = otherBlockchainSetup.getBlockchain();

    protocolSchedule = localBlockchainSetup.getProtocolSchedule();
    protocolContext = localBlockchainSetup.getProtocolContext();
    ethProtocolManager =
        EthProtocolManagerTestUtil.create(
            localBlockchain, new EthScheduler(1, 1, 1, 1, new NoOpMetricsSystem()));

    ethContext = ethProtocolManager.ethContext();
    syncState = new SyncState(protocolContext.getBlockchain(), ethContext.getEthPeers());
  }

  @After
  public void tearDown() {
    ethProtocolManager.stop();
  }

  private ChainDownloader downloader(
      final SynchronizerConfiguration syncConfig, final long pivotBlockNumber) {
    return FastSyncChainDownloader.create(
        syncConfig,
        protocolSchedule,
        protocolContext,
        ethContext,
        syncState,
        new NoOpMetricsSystem(),
        otherBlockchain.getBlockHeader(pivotBlockNumber).get());
  }

  @Test
  public void shouldSyncToPivotBlockInMultipleSegments() {
    otherBlockchainSetup.importFirstBlocks(30);

    final RespondingEthPeer peer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, otherBlockchain);
    final RespondingEthPeer.Responder responder =
        RespondingEthPeer.blockchainResponder(otherBlockchain);

    final SynchronizerConfiguration syncConfig =
        SynchronizerConfiguration.builder()
            .downloaderChainSegmentSize(5)
            .downloaderHeadersRequestSize(3)
            .build();
    final long pivotBlockNumber = 25;
    final ChainDownloader downloader = downloader(syncConfig, pivotBlockNumber);
    final CompletableFuture<Void> result = downloader.start();

    peer.respondWhileOtherThreadsWork(responder, () -> !result.isDone());

    assertThat(result).isCompleted();
    assertThat(localBlockchain.getChainHeadBlockNumber()).isEqualTo(pivotBlockNumber);
    assertThat(localBlockchain.getChainHeadHeader())
        .isEqualTo(otherBlockchain.getBlockHeader(pivotBlockNumber).get());
  }

  @Test
  public void shouldSyncToPivotBlockInSingleSegment() {
    otherBlockchainSetup.importFirstBlocks(30);

    final RespondingEthPeer peer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, otherBlockchain);
    final RespondingEthPeer.Responder responder =
        RespondingEthPeer.blockchainResponder(otherBlockchain);

    final long pivotBlockNumber = 5;
    final SynchronizerConfiguration syncConfig = SynchronizerConfiguration.builder().build();
    final ChainDownloader downloader = downloader(syncConfig, pivotBlockNumber);
    final CompletableFuture<Void> result = downloader.start();

    peer.respondWhileOtherThreadsWork(responder, () -> !result.isDone());

    assertThat(result).isCompleted();
    assertThat(localBlockchain.getChainHeadBlockNumber()).isEqualTo(pivotBlockNumber);
    assertThat(localBlockchain.getChainHeadHeader())
        .isEqualTo(otherBlockchain.getBlockHeader(pivotBlockNumber).get());
  }

  @Test
  public void recoversFromSyncTargetDisconnect() {
    final BlockchainSetupUtil shorterChainUtil = BlockchainSetupUtil.forTesting(storageFormat);
    final MutableBlockchain shorterChain = shorterChainUtil.getBlockchain();

    otherBlockchainSetup.importFirstBlocks(30);
    shorterChainUtil.importFirstBlocks(28);

    final RespondingEthPeer bestPeer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, otherBlockchain);
    final RespondingEthPeer secondBestPeer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, shorterChain);
    final RespondingEthPeer.Responder shorterResponder =
        RespondingEthPeer.blockchainResponder(shorterChain);
    // Doesn't respond to requests for checkpoints unless it's starting from geneis
    // So the import can only make it as far as block 15 (3 checkpoints 5 blocks apart)
    final RespondingEthPeer.Responder shorterLimitedRangeResponder =
        RespondingEthPeer.targetedResponder(
            (cap, msg) -> {
              if (msg.getCode() == EthPV62.GET_BLOCK_HEADERS) {
                final GetBlockHeadersMessage request = GetBlockHeadersMessage.readFrom(msg);
                return request.skip() == 0
                    || (request.hash().equals(localBlockchain.getBlockHashByNumber(0)));
              } else {
                return true;
              }
            },
            (cap, msg) -> shorterResponder.respond(cap, msg).get());

    final SynchronizerConfiguration syncConfig =
        SynchronizerConfiguration.builder()
            .downloaderChainSegmentSize(5)
            .downloaderHeadersRequestSize(3)
            .downloaderParallelism(1)
            .build();
    final long pivotBlockNumber = 25;
    final ChainDownloader downloader = downloader(syncConfig, pivotBlockNumber);
    final CompletableFuture<Void> result = downloader.start();

    while (localBlockchain.getChainHeadBlockNumber() < 15) {
      bestPeer.respond(shorterLimitedRangeResponder);
      secondBestPeer.respond(shorterLimitedRangeResponder);
      LockSupport.parkNanos(200);
    }

    assertThat(localBlockchain.getChainHeadBlockNumber()).isEqualTo(15);
    assertThat(result).isNotCompleted();

    ethProtocolManager.handleDisconnect(bestPeer.getPeerConnection(), TOO_MANY_PEERS, true);

    secondBestPeer.respondWhileOtherThreadsWork(shorterResponder, () -> !result.isDone());

    assertThat(result).isCompleted();
    assertThat(localBlockchain.getChainHeadBlockNumber()).isEqualTo(pivotBlockNumber);
    assertThat(localBlockchain.getChainHeadHeader())
        .isEqualTo(otherBlockchain.getBlockHeader(pivotBlockNumber).get());
  }
}
