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
package org.enterchain.enter.ethereum.core;

import org.enterchain.enter.ethereum.bonsai.BonsaiWorldStateArchive;
import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.chain.DefaultBlockchain;
import org.enterchain.enter.ethereum.chain.MutableBlockchain;
import org.enterchain.enter.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.enterchain.enter.ethereum.privacy.storage.PrivateStateKeyValueStorage;
import org.enterchain.enter.ethereum.privacy.storage.PrivateStateStorage;
import org.enterchain.enter.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage;
import org.enterchain.enter.ethereum.storage.keyvalue.KeyValueStorageProvider;
import org.enterchain.enter.ethereum.storage.keyvalue.WorldStateKeyValueStorage;
import org.enterchain.enter.ethereum.storage.keyvalue.WorldStatePreimageKeyValueStorage;
import org.enterchain.enter.ethereum.worldstate.DataStorageFormat;
import org.enterchain.enter.ethereum.worldstate.DefaultMutableWorldState;
import org.enterchain.enter.ethereum.worldstate.DefaultWorldStateArchive;
import org.enterchain.enter.metrics.noop.NoOpMetricsSystem;
import org.enterchain.enter.services.kvstore.InMemoryKeyValueStorage;

public class InMemoryKeyValueStorageProvider extends KeyValueStorageProvider {

  public InMemoryKeyValueStorageProvider() {
    super(
        segmentIdentifier -> new InMemoryKeyValueStorage(),
        new InMemoryKeyValueStorage(),
        new InMemoryKeyValueStorage(),
        true);
  }

  public static MutableBlockchain createInMemoryBlockchain(final Block genesisBlock) {
    return createInMemoryBlockchain(genesisBlock, new MainnetBlockHeaderFunctions());
  }

  public static MutableBlockchain createInMemoryBlockchain(
      final Block genesisBlock, final BlockHeaderFunctions blockHeaderFunctions) {
    final InMemoryKeyValueStorage keyValueStorage = new InMemoryKeyValueStorage();
    return DefaultBlockchain.createMutable(
        genesisBlock,
        new KeyValueStoragePrefixedKeyBlockchainStorage(keyValueStorage, blockHeaderFunctions),
        new NoOpMetricsSystem(),
        0);
  }

  public static DefaultWorldStateArchive createInMemoryWorldStateArchive() {
    return new DefaultWorldStateArchive(
        new WorldStateKeyValueStorage(new InMemoryKeyValueStorage()),
        new WorldStatePreimageKeyValueStorage(new InMemoryKeyValueStorage()));
  }

  public static BonsaiWorldStateArchive createBonsaiInMemoryWorldStateArchive(
      final Blockchain blockchain) {
    return new BonsaiWorldStateArchive(new InMemoryKeyValueStorageProvider(), blockchain);
  }

  public static MutableWorldState createInMemoryWorldState() {
    final InMemoryKeyValueStorageProvider provider = new InMemoryKeyValueStorageProvider();
    return new DefaultMutableWorldState(
        provider.createWorldStateStorage(DataStorageFormat.FOREST),
        provider.createWorldStatePreimageStorage());
  }

  public static PrivateStateStorage createInMemoryPrivateStateStorage() {
    return new PrivateStateKeyValueStorage(new InMemoryKeyValueStorage());
  }
}
