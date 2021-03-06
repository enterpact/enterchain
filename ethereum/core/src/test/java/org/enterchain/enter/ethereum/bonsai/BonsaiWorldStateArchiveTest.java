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
 *
 */
package org.enterchain.enter.ethereum.bonsai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.enterchain.enter.ethereum.bonsai.BonsaiWorldStateKeyValueStorage.WORLD_BLOCK_HASH_KEY;
import static org.enterchain.enter.ethereum.bonsai.BonsaiWorldStateKeyValueStorage.WORLD_ROOT_HASH_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.BlockHeaderTestFixture;
import org.enterchain.enter.ethereum.rlp.BytesValueRLPOutput;
import org.enterchain.enter.ethereum.storage.StorageProvider;
import org.enterchain.enter.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.enterchain.enter.plugin.services.storage.KeyValueStorage;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BonsaiWorldStateArchiveTest {

  final BlockHeaderTestFixture blockBuilder = new BlockHeaderTestFixture();

  @Mock Blockchain blockchain;

  @Mock StorageProvider storageProvider;

  @Mock KeyValueStorage keyValueStorage;

  BonsaiWorldStateArchive bonsaiWorldStateArchive;

  @Before
  public void setUp() {
    when(storageProvider.getStorageBySegmentIdentifier(any(KeyValueSegmentIdentifier.class)))
        .thenReturn(keyValueStorage);
  }

  @Test
  public void testGetMutableReturnPersistedStateWhenNeeded() {
    final BlockHeader chainHead = blockBuilder.number(0).buildHeader();

    when(keyValueStorage.get(WORLD_ROOT_HASH_KEY))
        .thenReturn(Optional.of(chainHead.getStateRoot().toArrayUnsafe()));
    when(keyValueStorage.get(WORLD_BLOCK_HASH_KEY))
        .thenReturn(Optional.of(chainHead.getHash().toArrayUnsafe()));
    when(keyValueStorage.get(WORLD_ROOT_HASH_KEY))
        .thenReturn(Optional.of(chainHead.getStateRoot().toArrayUnsafe()));
    when(keyValueStorage.get(WORLD_BLOCK_HASH_KEY))
        .thenReturn(Optional.of(chainHead.getHash().toArrayUnsafe()));
    bonsaiWorldStateArchive = new BonsaiWorldStateArchive(storageProvider, blockchain, 1);

    assertThat(bonsaiWorldStateArchive.getMutable(null, chainHead.getHash(), true))
        .containsInstanceOf(BonsaiPersistedWorldState.class);
  }

  @Test
  public void testGetMutableReturnEmptyWhenLoadMoreThanLimitLayersBack() {
    bonsaiWorldStateArchive = new BonsaiWorldStateArchive(storageProvider, blockchain, 512);
    final BlockHeader blockHeader = blockBuilder.number(0).buildHeader();
    final BlockHeader chainHead = blockBuilder.number(512).buildHeader();
    when(blockchain.getBlockHeader(eq(blockHeader.getHash()))).thenReturn(Optional.of(blockHeader));
    when(blockchain.getChainHeadHeader()).thenReturn(chainHead);
    assertThat(bonsaiWorldStateArchive.getMutable(null, blockHeader.getHash(), false)).isEmpty();
  }

  @Test
  public void testGetMutableWhenLoadLessThanLimitLayersBack() {
    bonsaiWorldStateArchive = new BonsaiWorldStateArchive(storageProvider, blockchain, 512);
    final BlockHeader blockHeader = blockBuilder.number(0).buildHeader();
    final BlockHeader chainHead = blockBuilder.number(511).buildHeader();

    final BytesValueRLPOutput rlpLog = new BytesValueRLPOutput();
    final TrieLogLayer trieLogLayer = new TrieLogLayer();
    trieLogLayer.setBlockHash(blockHeader.getHash());
    trieLogLayer.writeTo(rlpLog);
    when(keyValueStorage.get(blockHeader.getHash().toArrayUnsafe()))
        .thenReturn(Optional.of(rlpLog.encoded().toArrayUnsafe()));

    when(blockchain.getBlockHeader(eq(blockHeader.getHash()))).thenReturn(Optional.of(blockHeader));
    when(blockchain.getChainHeadHeader()).thenReturn(chainHead);
    assertThat(bonsaiWorldStateArchive.getMutable(null, blockHeader.getHash(), false))
        .containsInstanceOf(BonsaiLayeredWorldState.class);
  }
}
