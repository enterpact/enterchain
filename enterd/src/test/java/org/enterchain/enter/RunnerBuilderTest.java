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
package org.enterchain.enter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.enterchain.enter.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryBlockchain;
import static org.enterchain.enter.ethereum.storage.keyvalue.KeyValueSegmentIdentifier.BLOCKCHAIN;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.enterchain.enter.cli.config.EthNetworkConfig;
import org.enterchain.enter.consensus.common.bft.BftEventQueue;
import org.enterchain.enter.consensus.common.bft.network.PeerConnectionTracker;
import org.enterchain.enter.consensus.common.bft.protocol.BftProtocolManager;
import org.enterchain.enter.consensus.ibft.protocol.IbftSubProtocol;
import org.enterchain.enter.controller.BesuController;
import org.enterchain.enter.crypto.KeyPairSecurityModule;
import org.enterchain.enter.crypto.NodeKey;
import org.enterchain.enter.crypto.SECP256K1;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.api.graphql.GraphQLConfiguration;
import org.enterchain.enter.ethereum.api.jsonrpc.JsonRpcConfiguration;
import org.enterchain.enter.ethereum.api.jsonrpc.websocket.WebSocketConfiguration;
import org.enterchain.enter.ethereum.blockcreation.MiningCoordinator;
import org.enterchain.enter.ethereum.chain.DefaultBlockchain;
import org.enterchain.enter.ethereum.chain.MutableBlockchain;
import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.BlockDataGenerator;
import org.enterchain.enter.ethereum.core.InMemoryKeyValueStorageProvider;
import org.enterchain.enter.ethereum.core.MiningParameters;
import org.enterchain.enter.ethereum.core.PrivacyParameters;
import org.enterchain.enter.ethereum.core.Synchronizer;
import org.enterchain.enter.ethereum.eth.manager.EthContext;
import org.enterchain.enter.ethereum.eth.manager.EthProtocolManager;
import org.enterchain.enter.ethereum.eth.manager.EthScheduler;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPool;
import org.enterchain.enter.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.p2p.config.SubProtocolConfiguration;
import org.enterchain.enter.ethereum.p2p.peers.EnodeURLImpl;
import org.enterchain.enter.ethereum.storage.StorageProvider;
import org.enterchain.enter.ethereum.storage.keyvalue.KeyValueStorageProvider;
import org.enterchain.enter.metrics.ObservableMetricsSystem;
import org.enterchain.enter.metrics.prometheus.MetricsConfiguration;
import org.enterchain.enter.nat.NatMethod;
import org.enterchain.enter.plugin.data.EnodeURL;
import org.enterchain.enter.services.PermissioningServiceImpl;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Stream;

import io.vertx.core.Vertx;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt64;
import org.ethereum.beacon.discovery.schema.NodeRecord;
import org.ethereum.beacon.discovery.schema.NodeRecordFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class RunnerBuilderTest {

  @Rule public TemporaryFolder dataDir = new TemporaryFolder();

  @Mock BesuController besuController;
  @Mock Vertx vertx;

  @Before
  public void setup() {
    final SubProtocolConfiguration subProtocolConfiguration = mock(SubProtocolConfiguration.class);
    final EthProtocolManager ethProtocolManager = mock(EthProtocolManager.class);
    final EthContext ethContext = mock(EthContext.class);
    final ProtocolContext protocolContext = mock(ProtocolContext.class);
    final NodeKey nodeKey =
        new NodeKey(new KeyPairSecurityModule(new SECP256K1().generateKeyPair()));

    when(subProtocolConfiguration.getProtocolManagers())
        .thenReturn(
            Collections.singletonList(
                new BftProtocolManager(
                    mock(BftEventQueue.class),
                    mock(PeerConnectionTracker.class),
                    IbftSubProtocol.IBFV1,
                    IbftSubProtocol.get().getName())));
    when(ethContext.getScheduler()).thenReturn(mock(EthScheduler.class));
    when(ethProtocolManager.ethContext()).thenReturn(ethContext);
    when(subProtocolConfiguration.getSubProtocols())
        .thenReturn(Collections.singletonList(new IbftSubProtocol()));
    when(protocolContext.getBlockchain()).thenReturn(mock(DefaultBlockchain.class));

    when(besuController.getProtocolManager()).thenReturn(ethProtocolManager);
    when(besuController.getSubProtocolConfiguration()).thenReturn(subProtocolConfiguration);
    when(besuController.getProtocolContext()).thenReturn(protocolContext);
    when(besuController.getProtocolSchedule()).thenReturn(mock(ProtocolSchedule.class));
    when(besuController.getNodeKey()).thenReturn(nodeKey);
    when(besuController.getMiningParameters()).thenReturn(mock(MiningParameters.class));
    when(besuController.getPrivacyParameters()).thenReturn(mock(PrivacyParameters.class));
    when(besuController.getTransactionPool()).thenReturn(mock(TransactionPool.class));
    when(besuController.getSynchronizer()).thenReturn(mock(Synchronizer.class));
    when(besuController.getMiningCoordinator()).thenReturn(mock(MiningCoordinator.class));
  }

  @Test
  public void enodeUrlShouldHaveAdvertisedHostWhenDiscoveryDisabled() {
    final String p2pAdvertisedHost = "172.0.0.1";
    final int p2pListenPort = 30302;

    final Runner runner =
        new RunnerBuilder()
            .p2pListenInterface("0.0.0.0")
            .p2pListenPort(p2pListenPort)
            .p2pAdvertisedHost(p2pAdvertisedHost)
            .p2pEnabled(true)
            .discovery(false)
            .besuController(besuController)
            .ethNetworkConfig(mock(EthNetworkConfig.class))
            .metricsSystem(mock(ObservableMetricsSystem.class))
            .jsonRpcConfiguration(mock(JsonRpcConfiguration.class))
            .permissioningService(mock(PermissioningServiceImpl.class))
            .graphQLConfiguration(mock(GraphQLConfiguration.class))
            .webSocketConfiguration(mock(WebSocketConfiguration.class))
            .metricsConfiguration(mock(MetricsConfiguration.class))
            .vertx(vertx)
            .dataDir(dataDir.getRoot().toPath())
            .storageProvider(mock(KeyValueStorageProvider.class))
            .forkIdSupplier(() -> Collections.singletonList(Bytes.EMPTY))
            .build();
    runner.start();

    final EnodeURL expectedEodeURL =
        EnodeURLImpl.builder()
            .ipAddress(p2pAdvertisedHost)
            .discoveryPort(0)
            .listeningPort(p2pListenPort)
            .nodeId(besuController.getNodeKey().getPublicKey().getEncoded())
            .build();
    assertThat(runner.getLocalEnode().orElseThrow()).isEqualTo(expectedEodeURL);
  }

  @Test
  public void movingAcrossProtocolSpecsUpdatesNodeRecord() {
    final BlockDataGenerator gen = new BlockDataGenerator();
    final String p2pAdvertisedHost = "172.0.0.1";
    final int p2pListenPort = 30301;
    final StorageProvider storageProvider = new InMemoryKeyValueStorageProvider();
    final Block genesisBlock = gen.genesisBlock();
    final MutableBlockchain blockchain =
        createInMemoryBlockchain(genesisBlock, new MainnetBlockHeaderFunctions());
    when(besuController.getProtocolContext().getBlockchain()).thenReturn(blockchain);
    final Runner runner =
        new RunnerBuilder()
            .discovery(true)
            .p2pListenInterface("0.0.0.0")
            .p2pListenPort(p2pListenPort)
            .p2pAdvertisedHost(p2pAdvertisedHost)
            .p2pEnabled(true)
            .natMethod(NatMethod.NONE)
            .besuController(besuController)
            .ethNetworkConfig(mock(EthNetworkConfig.class))
            .metricsSystem(mock(ObservableMetricsSystem.class))
            .permissioningService(mock(PermissioningServiceImpl.class))
            .jsonRpcConfiguration(mock(JsonRpcConfiguration.class))
            .graphQLConfiguration(mock(GraphQLConfiguration.class))
            .webSocketConfiguration(mock(WebSocketConfiguration.class))
            .metricsConfiguration(mock(MetricsConfiguration.class))
            .vertx(Vertx.vertx())
            .dataDir(dataDir.getRoot().toPath())
            .storageProvider(storageProvider)
            .forkIdSupplier(() -> Collections.singletonList(Bytes.EMPTY))
            .build();
    runner.start();
    when(besuController.getProtocolSchedule().streamMilestoneBlocks())
        .thenAnswer(__ -> Stream.of(1L, 2L));
    for (int i = 0; i < 2; ++i) {
      final Block block =
          gen.block(
              BlockDataGenerator.BlockOptions.create()
                  .setBlockNumber(1 + i)
                  .setParentHash(blockchain.getChainHeadHash()));
      blockchain.appendBlock(block, gen.receipts(block));
      assertThat(
              storageProvider
                  .getStorageBySegmentIdentifier(BLOCKCHAIN)
                  .get("local-enr-seqno".getBytes(StandardCharsets.UTF_8))
                  .map(Bytes::of)
                  .map(NodeRecordFactory.DEFAULT::fromBytes)
                  .map(NodeRecord::getSeq))
          .contains(UInt64.valueOf(2 + i));
    }
  }
}
