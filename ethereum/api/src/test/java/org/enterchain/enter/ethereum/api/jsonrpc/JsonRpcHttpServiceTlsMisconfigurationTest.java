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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.enterchain.enter.ethereum.api.jsonrpc.RpcApis.ETH;
import static org.enterchain.enter.ethereum.api.jsonrpc.RpcApis.NET;
import static org.enterchain.enter.ethereum.api.jsonrpc.RpcApis.WEB3;
import static org.enterchain.enter.ethereum.api.tls.KnownClientFileUtil.writeToKnownClientsFile;
import static org.enterchain.enter.ethereum.api.tls.TlsClientAuthConfiguration.Builder.aTlsClientAuthConfiguration;
import static org.enterchain.enter.ethereum.api.tls.TlsConfiguration.Builder.aTlsConfiguration;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import org.enterchain.enter.config.StubGenesisConfigOptions;
import org.enterchain.enter.ethereum.api.jsonrpc.health.HealthService;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.filter.FilterManager;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.methods.JsonRpcMethodsFactory;
import org.enterchain.enter.ethereum.api.jsonrpc.websocket.WebSocketConfiguration;
import org.enterchain.enter.ethereum.api.query.BlockchainQueries;
import org.enterchain.enter.ethereum.api.tls.FileBasedPasswordProvider;
import org.enterchain.enter.ethereum.api.tls.SelfSignedP12Certificate;
import org.enterchain.enter.ethereum.api.tls.TlsConfiguration;
import org.enterchain.enter.ethereum.blockcreation.PoWMiningCoordinator;
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

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;

import io.vertx.core.Vertx;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JsonRpcHttpServiceTlsMisconfigurationTest {
  @ClassRule public static final TemporaryFolder folder = new TemporaryFolder();

  protected static final Vertx vertx = Vertx.vertx();

  private static final String CLIENT_VERSION = "TestClientVersion/0.1.0";
  private static final BigInteger CHAIN_ID = BigInteger.valueOf(123);
  private static final Collection<RpcApi> JSON_RPC_APIS = List.of(ETH, NET, WEB3);
  private static final NatService natService = new NatService(Optional.empty());
  private final SelfSignedP12Certificate besuCertificate = SelfSignedP12Certificate.create();
  private Path knownClientsFile;
  private Map<String, JsonRpcMethod> rpcMethods;
  private JsonRpcHttpService service;

  @Before
  public void beforeEach() throws IOException {
    knownClientsFile = folder.newFile().toPath();
    writeToKnownClientsFile(
        besuCertificate.getCommonName(),
        besuCertificate.getCertificateHexFingerprint(),
        knownClientsFile);
    final P2PNetwork peerDiscoveryMock = mock(P2PNetwork.class);
    final BlockchainQueries blockchainQueries = mock(BlockchainQueries.class);
    final Synchronizer synchronizer = mock(Synchronizer.class);

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
                    Collections.emptyMap(),
                    folder.getRoot().toPath(),
                    mock(EthPeers.class)));
  }

  @After
  public void shutdownServer() {
    Optional.ofNullable(service).ifPresent(s -> service.stop().join());
  }

  @Test
  public void exceptionRaisedWhenNonExistentKeystoreFileIsSpecified() throws IOException {
    service =
        createJsonRpcHttpService(
            rpcMethods, createJsonRpcConfig(invalidKeystorePathTlsConfiguration()));
    assertThatExceptionOfType(CompletionException.class)
        .isThrownBy(
            () -> {
              service.start().join();
              Assertions.fail("service.start should have failed");
            })
        .withCauseInstanceOf(JsonRpcServiceException.class);
  }

  @Test
  public void exceptionRaisedWhenIncorrectKeystorePasswordIsSpecified() throws IOException {
    service =
        createJsonRpcHttpService(
            rpcMethods, createJsonRpcConfig(invalidPasswordTlsConfiguration()));
    assertThatExceptionOfType(CompletionException.class)
        .isThrownBy(
            () -> {
              service.start().join();
              Assertions.fail("service.start should have failed");
            })
        .withCauseInstanceOf(JsonRpcServiceException.class)
        .withMessageContaining("failed to decrypt safe contents entry");
  }

  @Test
  public void exceptionRaisedWhenIncorrectKeystorePasswordFileIsSpecified() throws IOException {
    service =
        createJsonRpcHttpService(
            rpcMethods, createJsonRpcConfig(invalidPasswordFileTlsConfiguration()));
    assertThatExceptionOfType(CompletionException.class)
        .isThrownBy(
            () -> {
              service.start().join();
              Assertions.fail("service.start should have failed");
            })
        .withCauseInstanceOf(JsonRpcServiceException.class)
        .withMessageContaining("Unable to read keystore password file");
  }

  @Test
  public void exceptionRaisedWhenInvalidKeystoreFileIsSpecified() throws IOException {
    service =
        createJsonRpcHttpService(
            rpcMethods, createJsonRpcConfig(invalidKeystoreFileTlsConfiguration()));
    assertThatExceptionOfType(CompletionException.class)
        .isThrownBy(
            () -> {
              service.start().join();
              Assertions.fail("service.start should have failed");
            })
        .withCauseInstanceOf(JsonRpcServiceException.class)
        .withMessageContaining("Short read of DER length");
  }

  @Test
  public void exceptionRaisedWhenInvalidKnownClientsFileIsSpecified() throws IOException {
    service =
        createJsonRpcHttpService(
            rpcMethods, createJsonRpcConfig(invalidKnownClientsTlsConfiguration()));
    assertThatExceptionOfType(CompletionException.class)
        .isThrownBy(
            () -> {
              service.start().join();
              Assertions.fail("service.start should have failed");
            })
        .withCauseInstanceOf(JsonRpcServiceException.class)
        .withMessageContaining("Invalid fingerprint in");
  }

  private TlsConfiguration invalidKeystoreFileTlsConfiguration() throws IOException {
    final File tempFile = folder.newFile();
    return aTlsConfiguration()
        .withKeyStorePath(tempFile.toPath())
        .withKeyStorePasswordSupplier(() -> "invalid_password")
        .withClientAuthConfiguration(
            aTlsClientAuthConfiguration().withKnownClientsFile(knownClientsFile).build())
        .build();
  }

  private TlsConfiguration invalidKeystorePathTlsConfiguration() {
    return aTlsConfiguration()
        .withKeyStorePath(Path.of("/tmp/invalidkeystore.pfx"))
        .withKeyStorePasswordSupplier(() -> "invalid_password")
        .withClientAuthConfiguration(
            aTlsClientAuthConfiguration().withKnownClientsFile(knownClientsFile).build())
        .build();
  }

  private TlsConfiguration invalidPasswordTlsConfiguration() {
    return aTlsConfiguration()
        .withKeyStorePath(besuCertificate.getKeyStoreFile())
        .withKeyStorePasswordSupplier(() -> "invalid_password")
        .withClientAuthConfiguration(
            aTlsClientAuthConfiguration().withKnownClientsFile(knownClientsFile).build())
        .build();
  }

  private TlsConfiguration invalidPasswordFileTlsConfiguration() {
    return TlsConfiguration.Builder.aTlsConfiguration()
        .withKeyStorePath(besuCertificate.getKeyStoreFile())
        .withKeyStorePasswordSupplier(
            new FileBasedPasswordProvider(Path.of("/tmp/invalid_password_file.txt")))
        .withClientAuthConfiguration(
            aTlsClientAuthConfiguration().withKnownClientsFile(knownClientsFile).build())
        .build();
  }

  private TlsConfiguration invalidKnownClientsTlsConfiguration() throws IOException {
    final Path tempKnownClientsFile = folder.newFile().toPath();
    Files.write(tempKnownClientsFile, List.of("cn invalid_sha256"));

    return TlsConfiguration.Builder.aTlsConfiguration()
        .withKeyStorePath(besuCertificate.getKeyStoreFile())
        .withKeyStorePasswordSupplier(() -> new String(besuCertificate.getPassword()))
        .withClientAuthConfiguration(
            aTlsClientAuthConfiguration().withKnownClientsFile(tempKnownClientsFile).build())
        .build();
  }

  private JsonRpcHttpService createJsonRpcHttpService(
      final Map<String, JsonRpcMethod> rpcMethods, final JsonRpcConfiguration jsonRpcConfig)
      throws IOException {
    return new JsonRpcHttpService(
        vertx,
        folder.newFolder().toPath(),
        jsonRpcConfig,
        new NoOpMetricsSystem(),
        natService,
        rpcMethods,
        HealthService.ALWAYS_HEALTHY,
        HealthService.ALWAYS_HEALTHY);
  }

  private JsonRpcConfiguration createJsonRpcConfig(final TlsConfiguration tlsConfiguration) {
    final JsonRpcConfiguration config = JsonRpcConfiguration.createDefault();
    config.setPort(0);
    config.setHostsAllowlist(Collections.singletonList("*"));
    config.setTlsConfiguration(Optional.of(tlsConfiguration));
    return config;
  }
}
