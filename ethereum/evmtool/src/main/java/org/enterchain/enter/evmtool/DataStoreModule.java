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
package org.enterchain.enter.evmtool;

import org.enterchain.enter.ethereum.chain.BlockchainStorage;
import org.enterchain.enter.ethereum.core.BlockHeaderFunctions;
import org.enterchain.enter.ethereum.storage.keyvalue.KeyValueSegmentIdentifier;
import org.enterchain.enter.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage;
import org.enterchain.enter.plugin.services.BesuConfiguration;
import org.enterchain.enter.plugin.services.MetricsSystem;
import org.enterchain.enter.plugin.services.storage.KeyValueStorage;
import org.enterchain.enter.plugin.services.storage.SegmentIdentifier;
import org.enterchain.enter.plugin.services.storage.rocksdb.RocksDBKeyValueStorageFactory;
import org.enterchain.enter.plugin.services.storage.rocksdb.RocksDBMetricsFactory;
import org.enterchain.enter.plugin.services.storage.rocksdb.configuration.RocksDBCLIOptions;
import org.enterchain.enter.services.kvstore.InMemoryKeyValueStorage;
import org.enterchain.enter.services.kvstore.LimitedInMemoryKeyValueStorage;

import java.util.List;
import java.util.function.Supplier;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.base.Suppliers;
import dagger.Module;
import dagger.Provides;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings({"CloseableProvides"})
@Module(includes = GenesisFileModule.class)
public class DataStoreModule {

  private static final Logger LOG = LogManager.getLogger();
  private final Supplier<RocksDBKeyValueStorageFactory> rocksDBFactory =
      Suppliers.memoize(
          () ->
              new RocksDBKeyValueStorageFactory(
                  RocksDBCLIOptions.create()::toDomainObject,
                  List.of(KeyValueSegmentIdentifier.values()),
                  RocksDBMetricsFactory.PUBLIC_ROCKS_DB_METRICS));

  @Provides
  @Singleton
  @Named("blockchain")
  KeyValueStorage provideBlockchainKeyValueStorage(
      @Named("KeyValueStorageName") final String keyValueStorageName,
      final BesuConfiguration commonConfiguration,
      final MetricsSystem metricsSystem) {
    return constructKeyValueStorage(
        keyValueStorageName,
        commonConfiguration,
        metricsSystem,
        KeyValueSegmentIdentifier.BLOCKCHAIN);
  }

  @Provides
  @Singleton
  @Named("worldState")
  KeyValueStorage provideWorldStateKeyValueStorage(
      @Named("KeyValueStorageName") final String keyValueStorageName,
      final BesuConfiguration commonConfiguration,
      final MetricsSystem metricsSystem) {
    return constructKeyValueStorage(
        keyValueStorageName,
        commonConfiguration,
        metricsSystem,
        KeyValueSegmentIdentifier.WORLD_STATE);
  }

  @Provides
  @Singleton
  @Named("worldStatePreimage")
  KeyValueStorage provideWorldStatePreimageKeyValueStorage(
      @Named("KeyValueStorageName") final String keyValueStorageName,
      final BesuConfiguration commonConfiguration,
      final MetricsSystem metricsSystem) {
    return new LimitedInMemoryKeyValueStorage(5000);
  }

  @Provides
  @Singleton
  @Named("pruning")
  KeyValueStorage providePruningKeyValueStorage(
      @Named("KeyValueStorageName") final String keyValueStorageName,
      final BesuConfiguration commonConfiguration,
      final MetricsSystem metricsSystem) {
    return constructKeyValueStorage(
        keyValueStorageName,
        commonConfiguration,
        metricsSystem,
        KeyValueSegmentIdentifier.PRUNING_STATE);
  }

  private KeyValueStorage constructKeyValueStorage(
      @Named("KeyValueStorageName") final String keyValueStorageName,
      final BesuConfiguration commonConfiguration,
      final MetricsSystem metricsSystem,
      final SegmentIdentifier segment) {

    switch (keyValueStorageName) {
      case "rocksdb":
        return rocksDBFactory.get().create(segment, commonConfiguration, metricsSystem);
      default:
        LOG.error("Unknown key, continuing as though 'memory' was specified");
        // fall through
      case "memory":
        return new InMemoryKeyValueStorage();
    }
  }

  @Provides
  @Singleton
  static BlockchainStorage provideBlockchainStorage(
      @Named("blockchain") final KeyValueStorage keyValueStorage,
      final BlockHeaderFunctions blockHashFunction) {
    return new KeyValueStoragePrefixedKeyBlockchainStorage(keyValueStorage, blockHashFunction);
  }
}
