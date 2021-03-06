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
package org.enterchain.enter.ethereum.eth.sync;

import static org.mockito.Mockito.mock;

import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.core.BlockchainSetupUtil;
import org.enterchain.enter.ethereum.eth.EthProtocolConfiguration;
import org.enterchain.enter.ethereum.eth.manager.EthProtocolManagerTestUtil;
import org.enterchain.enter.ethereum.eth.sync.state.SyncState;
import org.enterchain.enter.ethereum.worldstate.DataStorageFormat;

import org.junit.Before;
import org.junit.BeforeClass;

public class BonsaiBlockPropagationManagerTest extends AbstractBlockPropagationManagerTest {

  private static Blockchain fullBlockchain;

  @BeforeClass
  public static void setupSuite() {
    fullBlockchain = BlockchainSetupUtil.forTesting(DataStorageFormat.BONSAI).importAllBlocks();
  }

  @Before
  public void setup() {
    blockchainUtil = BlockchainSetupUtil.forTesting(DataStorageFormat.BONSAI);
    blockchain = blockchainUtil.getBlockchain();
    protocolSchedule = blockchainUtil.getProtocolSchedule();
    final ProtocolContext tempProtocolContext = blockchainUtil.getProtocolContext();
    protocolContext =
        new ProtocolContext(
            blockchain,
            tempProtocolContext.getWorldStateArchive(),
            tempProtocolContext.getConsensusState(Object.class));
    ethProtocolManager =
        EthProtocolManagerTestUtil.create(
            blockchain,
            blockchainUtil.getWorldArchive(),
            blockchainUtil.getTransactionPool(),
            EthProtocolConfiguration.defaultConfig());
    syncConfig = SynchronizerConfiguration.builder().blockPropagationRange(-3, 5).build();
    syncState = new SyncState(blockchain, ethProtocolManager.ethContext().getEthPeers());
    blockBroadcaster = mock(BlockBroadcaster.class);
    blockPropagationManager =
        new BlockPropagationManager(
            syncConfig,
            protocolSchedule,
            protocolContext,
            ethProtocolManager.ethContext(),
            syncState,
            pendingBlocksManager,
            metricsSystem,
            blockBroadcaster);
  }

  @Override
  public Blockchain getFullBlockchain() {
    return fullBlockchain;
  }
}
