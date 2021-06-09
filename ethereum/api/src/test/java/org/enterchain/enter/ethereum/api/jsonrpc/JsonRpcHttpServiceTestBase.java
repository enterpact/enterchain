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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import org.enterchain.enter.config.StubGenesisConfigOptions;
import org.enterchain.enter.ethereum.api.jsonrpc.health.HealthService;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.filter.FilterManager;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.methods.JsonRpcMethodsFactory;
import org.enterchain.enter.ethereum.api.jsonrpc.websocket.WebSocketConfiguration;
import org.enterchain.enter.ethereum.api.query.BlockchainQueries;
import org.enterchain.enter.ethereum.blockcreation.PoWMiningCoordinator;
import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.chain.ChainHead;
import org.enterchain.enter.ethereum.core.PrivacyParameters;
import org.enterchain.enter.ethereum.core.Synchronizer;
import org.enterchain.enter.ethereum.eth.EthProtocol;
import org.enterchain.enter.ethereum.eth.manager.EthPeers;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPool;
import org.enterchain.enter.ethereum.mainnet.MainnetProtocolSchedule;
import org.enterchain.enter.ethereum.p2p.network.P2PNetwork;
import org.enterchain.enter.ethereum.p2p.rlpx.wire.Capability;
import org.enterchain.enter.ethereum.permissioning.AccountLocalConfigPermissioningController;
import org.enterchain.enter.ethereum.permissioning.NodeLocalConfigPermissioningController;
import org.enterchain.enter.metrics.noop.NoOpMetricsSystem;
import org.enterchain.enter.metrics.prometheus.MetricsConfiguration;
import org.enterchain.enter.nat.NatService;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.vertx.core.Vertx;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;

public class JsonRpcHttpServiceTestBase {

  @ClassRule public static final TemporaryFolder folder = new TemporaryFolder();
  protected final JsonRpcTestHelper testHelper = new JsonRpcTestHelper();

  private static final Vertx vertx = Vertx.vertx();

  protected static Map<String, JsonRpcMethod> rpcMethods;
  protected static JsonRpcHttpService service;
  protected static OkHttpClient client;
  protected static String baseUrl;
  protected static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
  protected static final String CLIENT_VERSION = "TestClientVersion/0.1.0";
  protected static final BigInteger CHAIN_ID = BigInteger.valueOf(123);
  protected static P2PNetwork peerDiscoveryMock;
  protected static EthPeers ethPeersMock;
  protected static Blockchain blockchain;
  protected static BlockchainQueries blockchainQueries;
  protected static ChainHead chainHead;
  protected static Synchronizer synchronizer;
  protected static final Collection<RpcApi> JSON_RPC_APIS =
      Arrays.asList(RpcApis.ETH, RpcApis.NET, RpcApis.WEB3, RpcApis.ADMIN);
  protected static final NatService natService = new NatService(Optional.empty());
  protected static int maxConnections = 80;

  public static void initServerAndClient() throws Exception {
    peerDiscoveryMock = mock(P2PNetwork.class);
    ethPeersMock = mock(EthPeers.class);
    blockchain = mock(Blockchain.class);
    blockchainQueries = mock(BlockchainQueries.class);
    chainHead = mock(ChainHead.class);
    synchronizer = mock(Synchronizer.class);

    final Set<Capability> supportedCapabilities = new HashSet<>();
    supportedCapabilities.add(EthProtocol.ETH62);
    supportedCapabilities.add(EthProtocol.ETH63);

    rpcMethods =
        spy(
            new JsonRpcMethodsFactory()
                .methods(
                    CLIENT_VERSION,
                    CHAIN_ID,
                    new StubGenesisConfigOptions(),
                    peerDiscoveryMock,
                    blockchainQueries,
                    synchronizer,
                    MainnetProtocolSchedule.fromConfig(
                        new StubGenesisConfigOptions().constantinopleBlock(0).chainId(CHAIN_ID)),
                    mock(FilterManager.class),
                    mock(TransactionPool.class),
                    mock(PoWMiningCoordinator.class),
                    new NoOpMetricsSystem(),
                    supportedCapabilities,
                    Optional.of(mock(AccountLocalConfigPermissioningController.class)),
                    Optional.of(mock(NodeLocalConfigPermissioningController.class)),
                    JSON_RPC_APIS,
                    mock(PrivacyParameters.class),
                    mock(JsonRpcConfiguration.class),
                    mock(WebSocketConfiguration.class),
                    mock(MetricsConfiguration.class),
                    natService,
                    new HashMap<>(),
                    folder.getRoot().toPath(),
                    ethPeersMock));
    service = createJsonRpcHttpService(createLimitedJsonRpcConfig());
    service.start().join();

    // Build an OkHttp client.
    client = new OkHttpClient();
    baseUrl = service.url();
  }

  protected static JsonRpcHttpService createJsonRpcHttpService(final JsonRpcConfiguration config)
      throws Exception {
    return new JsonRpcHttpService(
        vertx,
        folder.newFolder().toPath(),
        config,
        new NoOpMetricsSystem(),
        natService,
        rpcMethods,
        HealthService.ALWAYS_HEALTHY,
        HealthService.ALWAYS_HEALTHY);
  }

  protected static JsonRpcHttpService createJsonRpcHttpService() throws Exception {
    return new JsonRpcHttpService(
        vertx,
        folder.newFolder().toPath(),
        createLimitedJsonRpcConfig(),
        new NoOpMetricsSystem(),
        natService,
        rpcMethods,
        HealthService.ALWAYS_HEALTHY,
        HealthService.ALWAYS_HEALTHY);
  }

  private static JsonRpcConfiguration createLimitedJsonRpcConfig() {
    final JsonRpcConfiguration config = JsonRpcConfiguration.createDefault();
    config.setPort(0);
    config.setHostsAllowlist(Collections.singletonList("*"));
    config.setMaxActiveConnections(maxConnections);
    return config;
  }

  protected Request buildPostRequest(final RequestBody body) {
    return new Request.Builder().post(body).url(baseUrl).build();
  }

  protected Request buildGetRequest(final String path) {
    return new Request.Builder().get().url(baseUrl + path).build();
  }

  /** Tears down the HTTP server. */
  @AfterClass
  public static void shutdownServer() {
    service.stop().join();
  }
}
