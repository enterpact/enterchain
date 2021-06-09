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
package org.enterchain.enter.ethereum.api.jsonrpc;

import static org.enterchain.enter.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryBlockchain;
import static org.enterchain.enter.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryWorldStateArchive;
import static org.mockito.Mockito.mock;

import org.enterchain.enter.config.StubGenesisConfigOptions;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.filter.FilterManager;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.filter.FilterManagerBuilder;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.methods.JsonRpcMethodsFactory;
import org.enterchain.enter.ethereum.api.jsonrpc.websocket.WebSocketConfiguration;
import org.enterchain.enter.ethereum.api.query.BlockchainQueries;
import org.enterchain.enter.ethereum.blockcreation.PoWMiningCoordinator;
import org.enterchain.enter.ethereum.chain.MutableBlockchain;
import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.BlockImporter;
import org.enterchain.enter.ethereum.core.PrivacyParameters;
import org.enterchain.enter.ethereum.core.ProtocolScheduleFixture;
import org.enterchain.enter.ethereum.core.Synchronizer;
import org.enterchain.enter.ethereum.eth.manager.EthPeers;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPool;
import org.enterchain.enter.ethereum.mainnet.HeaderValidationMode;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.mainnet.ProtocolSpec;
import org.enterchain.enter.ethereum.p2p.network.P2PNetwork;
import org.enterchain.enter.ethereum.permissioning.AccountLocalConfigPermissioningController;
import org.enterchain.enter.ethereum.permissioning.NodeLocalConfigPermissioningController;
import org.enterchain.enter.ethereum.worldstate.WorldStateArchive;
import org.enterchain.enter.metrics.ObservableMetricsSystem;
import org.enterchain.enter.metrics.noop.NoOpMetricsSystem;
import org.enterchain.enter.metrics.prometheus.MetricsConfiguration;
import org.enterchain.enter.nat.NatService;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Provides a facade to construct the JSON-RPC component. */
public class JsonRpcTestMethodsFactory {

  private static final String CLIENT_VERSION = "TestClientVersion/0.1.0";
  private static final BigInteger NETWORK_ID = BigInteger.valueOf(123);

  private final BlockchainImporter importer;

  public JsonRpcTestMethodsFactory(final BlockchainImporter importer) {
    this.importer = importer;
  }

  public Map<String, JsonRpcMethod> methods() {
    final WorldStateArchive stateArchive = createInMemoryWorldStateArchive();

    importer.getGenesisState().writeStateTo(stateArchive.getMutable());

    final MutableBlockchain blockchain = createInMemoryBlockchain(importer.getGenesisBlock());
    final ProtocolContext context = new ProtocolContext(blockchain, stateArchive, null);

    for (final Block block : importer.getBlocks()) {
      final ProtocolSchedule protocolSchedule = importer.getProtocolSchedule();
      final ProtocolSpec protocolSpec =
          protocolSchedule.getByBlockNumber(block.getHeader().getNumber());
      final BlockImporter blockImporter = protocolSpec.getBlockImporter();
      blockImporter.importBlock(context, block, HeaderValidationMode.FULL);
    }

    final BlockchainQueries blockchainQueries = new BlockchainQueries(blockchain, stateArchive);

    final Synchronizer synchronizer = mock(Synchronizer.class);
    final P2PNetwork peerDiscovery = mock(P2PNetwork.class);
    final EthPeers ethPeers = mock(EthPeers.class);
    final TransactionPool transactionPool = mock(TransactionPool.class);
    final PoWMiningCoordinator miningCoordinator = mock(PoWMiningCoordinator.class);
    final ObservableMetricsSystem metricsSystem = new NoOpMetricsSystem();
    final Optional<AccountLocalConfigPermissioningController> accountWhitelistController =
        Optional.of(mock(AccountLocalConfigPermissioningController.class));
    final Optional<NodeLocalConfigPermissioningController> nodeWhitelistController =
        Optional.of(mock(NodeLocalConfigPermissioningController.class));
    final PrivacyParameters privacyParameters = mock(PrivacyParameters.class);

    final FilterManager filterManager =
        new FilterManagerBuilder()
            .blockchainQueries(blockchainQueries)
            .transactionPool(transactionPool)
            .privacyParameters(privacyParameters)
            .build();

    final JsonRpcConfiguration jsonRpcConfiguration = mock(JsonRpcConfiguration.class);
    final WebSocketConfiguration webSocketConfiguration = mock(WebSocketConfiguration.class);
    final MetricsConfiguration metricsConfiguration = mock(MetricsConfiguration.class);
    final NatService natService = new NatService(Optional.empty());

    final List<RpcApi> apis = new ArrayList<>();
    apis.add(RpcApis.ETH);
    apis.add(RpcApis.NET);
    apis.add(RpcApis.WEB3);
    apis.add(RpcApis.PRIV);
    apis.add(RpcApis.DEBUG);

    final Path dataDir = mock(Path.class);

    return new JsonRpcMethodsFactory()
        .methods(
            CLIENT_VERSION,
            NETWORK_ID,
            new StubGenesisConfigOptions(),
            peerDiscovery,
            blockchainQueries,
            synchronizer,
            ProtocolScheduleFixture.MAINNET,
            filterManager,
            transactionPool,
            miningCoordinator,
            metricsSystem,
            new HashSet<>(),
            accountWhitelistController,
            nodeWhitelistController,
            apis,
            privacyParameters,
            jsonRpcConfiguration,
            webSocketConfiguration,
            metricsConfiguration,
            natService,
            new HashMap<>(),
            dataDir,
            ethPeers);
  }
}
