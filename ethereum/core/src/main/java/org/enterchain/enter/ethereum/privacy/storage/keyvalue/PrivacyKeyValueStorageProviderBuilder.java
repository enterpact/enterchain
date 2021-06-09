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
package org.enterchain.enter.ethereum.privacy.storage.keyvalue;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.enterchain.enter.ethereum.storage.keyvalue.KeyValueSegmentIdentifier.PRIVATE_STATE;
import static org.enterchain.enter.ethereum.storage.keyvalue.KeyValueSegmentIdentifier.PRIVATE_TRANSACTIONS;

import org.enterchain.enter.plugin.services.BesuConfiguration;
import org.enterchain.enter.plugin.services.MetricsSystem;
import org.enterchain.enter.plugin.services.storage.PrivacyKeyValueStorageFactory;
import org.enterchain.enter.services.kvstore.LimitedInMemoryKeyValueStorage;

public class PrivacyKeyValueStorageProviderBuilder {

  private static final long DEFAULT_WORLD_STATE_PRE_IMAGE_CACHE_SIZE = 5_000L;

  private PrivacyKeyValueStorageFactory storageFactory;
  private BesuConfiguration commonConfiguration;
  private MetricsSystem metricsSystem;

  public PrivacyKeyValueStorageProviderBuilder withStorageFactory(
      final PrivacyKeyValueStorageFactory storageFactory) {
    this.storageFactory = storageFactory;
    return this;
  }

  public PrivacyKeyValueStorageProviderBuilder withCommonConfiguration(
      final BesuConfiguration commonConfiguration) {
    this.commonConfiguration = commonConfiguration;
    return this;
  }

  public PrivacyKeyValueStorageProviderBuilder withMetricsSystem(
      final MetricsSystem metricsSystem) {
    this.metricsSystem = metricsSystem;
    return this;
  }

  public PrivacyKeyValueStorageProvider build() {
    checkNotNull(storageFactory, "Cannot build a storage provider without a storage factory.");
    checkNotNull(
        commonConfiguration,
        "Cannot build a storage provider without the plugin common configuration.");
    checkNotNull(metricsSystem, "Cannot build a storage provider without a metrics system.");

    return new PrivacyKeyValueStorageProvider(
        storageFactory.create(PRIVATE_TRANSACTIONS, commonConfiguration, metricsSystem),
        new LimitedInMemoryKeyValueStorage(DEFAULT_WORLD_STATE_PRE_IMAGE_CACHE_SIZE),
        storageFactory.create(PRIVATE_STATE, commonConfiguration, metricsSystem),
        storageFactory.getVersion());
  }
}
