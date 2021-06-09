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
package org.enterchain.enter.ethereum.storage.keyvalue;

import org.enterchain.enter.ethereum.bonsai.BonsaiWorldStateKeyValueStorage;
import org.enterchain.enter.ethereum.chain.BlockchainStorage;
import org.enterchain.enter.ethereum.goquorum.GoQuorumKeyValueStorage;
import org.enterchain.enter.ethereum.goquorum.GoQuorumPrivateStorage;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.mainnet.ScheduleBasedBlockHeaderFunctions;
import org.enterchain.enter.ethereum.storage.StorageProvider;
import org.enterchain.enter.ethereum.worldstate.DataStorageFormat;
import org.enterchain.enter.ethereum.worldstate.WorldStatePreimageStorage;
import org.enterchain.enter.ethereum.worldstate.WorldStateStorage;
import org.enterchain.enter.plugin.services.storage.KeyValueStorage;
import org.enterchain.enter.plugin.services.storage.SegmentIdentifier;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class KeyValueStorageProvider implements StorageProvider {

  private final Function<SegmentIdentifier, KeyValueStorage> storageCreator;
  private final KeyValueStorage worldStatePreimageStorage;
  private final KeyValueStorage privateWorldStatePreimageStorage;
  private final boolean isWorldStateIterable;
  private final Map<SegmentIdentifier, KeyValueStorage> storageInstances = new HashMap<>();

  public KeyValueStorageProvider(
      final Function<SegmentIdentifier, KeyValueStorage> storageCreator,
      final KeyValueStorage worldStatePreimageStorage,
      final boolean segmentIsolationSupported) {
    this.storageCreator = storageCreator;
    this.worldStatePreimageStorage = worldStatePreimageStorage;
    this.privateWorldStatePreimageStorage = null;
    this.isWorldStateIterable = segmentIsolationSupported;
  }

  public KeyValueStorageProvider(
      final Function<SegmentIdentifier, KeyValueStorage> storageCreator,
      final KeyValueStorage worldStatePreimageStorage,
      final KeyValueStorage privateWorldStatePreimageStorage,
      final boolean segmentIsolationSupported) {
    this.storageCreator = storageCreator;
    this.worldStatePreimageStorage = worldStatePreimageStorage;
    this.privateWorldStatePreimageStorage = privateWorldStatePreimageStorage;
    this.isWorldStateIterable = segmentIsolationSupported;
  }

  @Override
  public BlockchainStorage createBlockchainStorage(final ProtocolSchedule protocolSchedule) {
    return new KeyValueStoragePrefixedKeyBlockchainStorage(
        getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.BLOCKCHAIN),
        ScheduleBasedBlockHeaderFunctions.create(protocolSchedule));
  }

  @Override
  public WorldStateStorage createWorldStateStorage(final DataStorageFormat dataStorageFormat) {
    if (dataStorageFormat.equals(DataStorageFormat.BONSAI)) {
      return new BonsaiWorldStateKeyValueStorage(this);
    } else {
      return new WorldStateKeyValueStorage(
          getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.WORLD_STATE));
    }
  }

  @Override
  public WorldStatePreimageStorage createWorldStatePreimageStorage() {
    return new WorldStatePreimageKeyValueStorage(worldStatePreimageStorage);
  }

  @Override
  public KeyValueStorage getStorageBySegmentIdentifier(final SegmentIdentifier segment) {
    return storageInstances.computeIfAbsent(segment, storageCreator);
  }

  @Override
  public WorldStateStorage createPrivateWorldStateStorage() {
    return new WorldStateKeyValueStorage(
        getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.GOQUORUM_PRIVATE_WORLD_STATE));
  }

  @Override
  public WorldStatePreimageStorage createPrivateWorldStatePreimageStorage() {
    return new WorldStatePreimageKeyValueStorage(privateWorldStatePreimageStorage);
  }

  @Override
  public GoQuorumPrivateStorage createGoQuorumPrivateStorage() {
    return new GoQuorumKeyValueStorage(
        getStorageBySegmentIdentifier(KeyValueSegmentIdentifier.GOQUORUM_PRIVATE_STORAGE));
  }

  @Override
  public boolean isWorldStateIterable() {
    return isWorldStateIterable;
  }

  @Override
  public void close() throws IOException {
    for (final KeyValueStorage kvs : storageInstances.values()) {
      kvs.close();
    }
  }
}
