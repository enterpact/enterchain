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
package org.enterchain.enter.ethereum.api.graphql;

import org.enterchain.enter.ethereum.api.graphql.internal.pojoadapter.NormalBlockAdapter;
import org.enterchain.enter.ethereum.api.query.BlockchainQueries;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.MutableWorldState;
import org.enterchain.enter.ethereum.p2p.rlpx.wire.Capability;

import java.util.Optional;
import java.util.Set;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;

public abstract class AbstractDataFetcherTest {

  DataFetcher<Optional<NormalBlockAdapter>> fetcher;

  @Mock protected Set<Capability> supportedCapabilities;

  @Mock protected DataFetchingEnvironment environment;

  @Mock protected GraphQLDataFetcherContextImpl context;

  @Mock protected BlockchainQueries query;

  @Mock protected BlockHeader header;

  @Mock protected MutableWorldState mutableWorldState;

  @Before
  public void before() {
    final GraphQLDataFetchers fetchers = new GraphQLDataFetchers(supportedCapabilities);
    fetcher = fetchers.getBlockDataFetcher();
    Mockito.when(environment.getContext()).thenReturn(context);
    Mockito.when(context.getBlockchainQueries()).thenReturn(query);
  }
}
