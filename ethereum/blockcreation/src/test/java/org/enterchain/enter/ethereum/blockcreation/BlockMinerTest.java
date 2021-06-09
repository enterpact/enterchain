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
package org.enterchain.enter.ethereum.blockcreation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.chain.MinedBlockObserver;
import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.BlockBody;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.BlockHeaderTestFixture;
import org.enterchain.enter.ethereum.core.BlockImporter;
import org.enterchain.enter.ethereum.mainnet.HeaderValidationMode;
import org.enterchain.enter.ethereum.mainnet.MutableProtocolSchedule;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.mainnet.ProtocolSpec;
import org.enterchain.enter.util.Subscribers;

import java.math.BigInteger;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.Lists;
import org.junit.Test;

public class BlockMinerTest {

  @Test
  public void blockCreatedIsAddedToBlockChain() throws InterruptedException {
    final BlockHeaderTestFixture headerBuilder = new BlockHeaderTestFixture();

    final Block blockToCreate =
        new Block(
            headerBuilder.buildHeader(), new BlockBody(Lists.newArrayList(), Lists.newArrayList()));

    final ProtocolContext protocolContext = new ProtocolContext(null, null, null);

    final PoWBlockCreator blockCreator = mock(PoWBlockCreator.class);
    final Function<BlockHeader, PoWBlockCreator> blockCreatorSupplier =
        (parentHeader) -> blockCreator;
    when(blockCreator.createBlock(anyLong())).thenReturn(blockToCreate);

    final BlockImporter blockImporter = mock(BlockImporter.class);
    final ProtocolSpec protocolSpec = mock(ProtocolSpec.class);

    final ProtocolSchedule protocolSchedule = singleSpecSchedule(protocolSpec);

    when(protocolSpec.getBlockImporter()).thenReturn(blockImporter);
    when(blockImporter.importBlock(any(), any(), any())).thenReturn(true);

    final MinedBlockObserver observer = mock(MinedBlockObserver.class);
    final DefaultBlockScheduler scheduler = mock(DefaultBlockScheduler.class);
    when(scheduler.waitUntilNextBlockCanBeMined(any())).thenReturn(5L);
    final BlockMiner<PoWBlockCreator> miner =
        new PoWBlockMiner(
            blockCreatorSupplier,
            protocolSchedule,
            protocolContext,
            subscribersContaining(observer),
            scheduler,
            headerBuilder.buildHeader()); // parent header is arbitrary for the test.

    miner.run();
    verify(blockImporter).importBlock(protocolContext, blockToCreate, HeaderValidationMode.FULL);
    verify(observer, times(1)).blockMined(blockToCreate);
  }

  @Test
  public void failureToImportDoesNotTriggerObservers() throws InterruptedException {
    final BlockHeaderTestFixture headerBuilder = new BlockHeaderTestFixture();

    final Block blockToCreate =
        new Block(
            headerBuilder.buildHeader(), new BlockBody(Lists.newArrayList(), Lists.newArrayList()));

    final ProtocolContext protocolContext = new ProtocolContext(null, null, null);

    final PoWBlockCreator blockCreator = mock(PoWBlockCreator.class);
    final Function<BlockHeader, PoWBlockCreator> blockCreatorSupplier =
        (parentHeader) -> blockCreator;
    when(blockCreator.createBlock(anyLong())).thenReturn(blockToCreate);

    final BlockImporter blockImporter = mock(BlockImporter.class);
    final ProtocolSpec protocolSpec = mock(ProtocolSpec.class);
    final ProtocolSchedule protocolSchedule = singleSpecSchedule(protocolSpec);

    when(protocolSpec.getBlockImporter()).thenReturn(blockImporter);
    when(blockImporter.importBlock(any(), any(), any())).thenReturn(false, false, true);

    final MinedBlockObserver observer = mock(MinedBlockObserver.class);
    final DefaultBlockScheduler scheduler = mock(DefaultBlockScheduler.class);
    when(scheduler.waitUntilNextBlockCanBeMined(any())).thenReturn(5L);
    final BlockMiner<PoWBlockCreator> miner =
        new PoWBlockMiner(
            blockCreatorSupplier,
            protocolSchedule,
            protocolContext,
            subscribersContaining(observer),
            scheduler,
            headerBuilder.buildHeader()); // parent header is arbitrary for the test.

    miner.run();
    verify(blockImporter, times(3))
        .importBlock(protocolContext, blockToCreate, HeaderValidationMode.FULL);
    verify(observer, times(1)).blockMined(blockToCreate);
  }

  private static Subscribers<MinedBlockObserver> subscribersContaining(
      final MinedBlockObserver... observers) {
    final Subscribers<MinedBlockObserver> result = Subscribers.create();
    for (final MinedBlockObserver obs : observers) {
      result.subscribe(obs);
    }
    return result;
  }

  private ProtocolSchedule singleSpecSchedule(final ProtocolSpec protocolSpec) {
    final MutableProtocolSchedule protocolSchedule =
        new MutableProtocolSchedule(Optional.of(BigInteger.valueOf(1234)));
    protocolSchedule.putMilestone(0, protocolSpec);
    return protocolSchedule;
  }
}
