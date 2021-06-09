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
package org.enterchain.enter.tests.acceptance.dsl.node;

import static org.enterchain.enter.cli.config.NetworkName.DEV;
import static org.enterchain.enter.controller.BesuController.DATABASE_PATH;

import org.enterchain.enter.Runner;
import org.enterchain.enter.RunnerBuilder;
import org.enterchain.enter.cli.config.EthNetworkConfig;
import org.enterchain.enter.controller.BesuController;
import org.enterchain.enter.controller.BesuControllerBuilder;
import org.enterchain.enter.crypto.KeyPairSecurityModule;
import org.enterchain.enter.crypto.KeyPairUtil;
import org.enterchain.enter.crypto.NodeKey;
import org.enterchain.enter.ethereum.api.graphql.GraphQLConfiguration;
import org.enterchain.enter.ethereum.blockcreation.GasLimitCalculator;
import org.enterchain.enter.ethereum.eth.EthProtocolConfiguration;
import org.enterchain.enter.ethereum.eth.sync.SynchronizerConfiguration;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.enterchain.enter.ethereum.p2p.peers.EnodeURLImpl;
import org.enterchain.enter.ethereum.storage.keyvalue.KeyValueStorageProvider;
import org.enterchain.enter.ethereum.storage.keyvalue.KeyValueStorageProviderBuilder;
import org.enterchain.enter.metrics.MetricsSystemFactory;
import org.enterchain.enter.metrics.ObservableMetricsSystem;
import org.enterchain.enter.plugin.data.EnodeURL;
import org.enterchain.enter.plugin.services.BesuConfiguration;
import org.enterchain.enter.plugin.services.BesuEvents;
import org.enterchain.enter.plugin.services.PicoCLIOptions;
import org.enterchain.enter.plugin.services.SecurityModuleService;
import org.enterchain.enter.plugin.services.StorageService;
import org.enterchain.enter.plugin.services.storage.rocksdb.RocksDBPlugin;
import org.enterchain.enter.services.BesuConfigurationImpl;
import org.enterchain.enter.services.BesuEventsImpl;
import org.enterchain.enter.services.BesuPluginContextImpl;
import org.enterchain.enter.services.PermissioningServiceImpl;
import org.enterchain.enter.services.PicoCLIOptionsImpl;
import org.enterchain.enter.services.SecurityModuleServiceImpl;
import org.enterchain.enter.services.StorageServiceImpl;

import java.io.File;
import java.nio.file.Path;
import java.time.Clock;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

public class ThreadBesuNodeRunner implements BesuNodeRunner {

  private static final Logger LOG = LogManager.getLogger();
  private final Map<String, Runner> besuRunners = new HashMap<>();

  private final Map<Node, BesuPluginContextImpl> besuPluginContextMap = new ConcurrentHashMap<>();

  private BesuPluginContextImpl buildPluginContext(
      final BesuNode node,
      final StorageServiceImpl storageService,
      final SecurityModuleServiceImpl securityModuleService,
      final BesuConfiguration commonPluginConfiguration) {
    final CommandLine commandLine = new CommandLine(CommandSpec.create());
    final BesuPluginContextImpl besuPluginContext = new BesuPluginContextImpl();
    besuPluginContext.addService(StorageService.class, storageService);
    besuPluginContext.addService(SecurityModuleService.class, securityModuleService);
    besuPluginContext.addService(PicoCLIOptions.class, new PicoCLIOptionsImpl(commandLine));

    final Path pluginsPath = node.homeDirectory().resolve("plugins");
    final File pluginsDirFile = pluginsPath.toFile();
    if (!pluginsDirFile.isDirectory()) {
      pluginsDirFile.mkdirs();
      pluginsDirFile.deleteOnExit();
    }
    System.setProperty("enter.plugins.dir", pluginsPath.toString());
    besuPluginContext.registerPlugins(pluginsPath);

    commandLine.parseArgs(node.getConfiguration().getExtraCLIOptions().toArray(new String[0]));

    besuPluginContext.addService(BesuConfiguration.class, commonPluginConfiguration);

    // register built-in plugins
    new RocksDBPlugin().register(besuPluginContext);

    return besuPluginContext;
  }

  @Override
  public void startNode(final BesuNode node) {

    if (ThreadContext.containsKey("node")) {
      LOG.error("ThreadContext node is already set to {}", ThreadContext.get("node"));
    }
    ThreadContext.put("node", node.getName());

    if (!node.getRunCommand().isEmpty()) {
      throw new UnsupportedOperationException("commands are not supported with thread runner");
    }

    final StorageServiceImpl storageService = new StorageServiceImpl();
    final SecurityModuleServiceImpl securityModuleService = new SecurityModuleServiceImpl();
    final Path dataDir = node.homeDirectory();
    final BesuConfiguration commonPluginConfiguration =
        new BesuConfigurationImpl(dataDir, dataDir.resolve(DATABASE_PATH));
    final BesuPluginContextImpl besuPluginContext =
        besuPluginContextMap.computeIfAbsent(
            node,
            n ->
                buildPluginContext(
                    node, storageService, securityModuleService, commonPluginConfiguration));

    final ObservableMetricsSystem metricsSystem =
        MetricsSystemFactory.create(node.getMetricsConfiguration());
    final List<EnodeURL> bootnodes =
        node.getConfiguration().getBootnodes().stream()
            .map(EnodeURLImpl::fromURI)
            .collect(Collectors.toList());
    final EthNetworkConfig.Builder networkConfigBuilder =
        new EthNetworkConfig.Builder(EthNetworkConfig.getNetworkConfig(DEV))
            .setBootNodes(bootnodes);
    node.getConfiguration().getGenesisConfig().ifPresent(networkConfigBuilder::setGenesisConfig);
    final EthNetworkConfig ethNetworkConfig = networkConfigBuilder.build();
    final BesuControllerBuilder builder =
        new BesuController.Builder().fromEthNetworkConfig(ethNetworkConfig);

    final KeyValueStorageProvider storageProvider =
        new KeyValueStorageProviderBuilder()
            .withStorageFactory(storageService.getByName("rocksdb").get())
            .withCommonConfiguration(commonPluginConfiguration)
            .withMetricsSystem(metricsSystem)
            .build();

    final BesuController besuController =
        builder
            .synchronizerConfiguration(new SynchronizerConfiguration.Builder().build())
            .dataDirectory(node.homeDirectory())
            .miningParameters(node.getMiningParameters())
            .privacyParameters(node.getPrivacyParameters())
            .nodeKey(new NodeKey(new KeyPairSecurityModule(KeyPairUtil.loadKeyPair(dataDir))))
            .metricsSystem(metricsSystem)
            .transactionPoolConfiguration(TransactionPoolConfiguration.DEFAULT)
            .ethProtocolConfiguration(EthProtocolConfiguration.defaultConfig())
            .clock(Clock.systemUTC())
            .isRevertReasonEnabled(node.isRevertReasonEnabled())
            .storageProvider(storageProvider)
            .gasLimitCalculator(GasLimitCalculator.constant())
            .build();

    final RunnerBuilder runnerBuilder = new RunnerBuilder();
    runnerBuilder.permissioningConfiguration(node.getPermissioningConfiguration());

    besuPluginContext.addService(
        BesuEvents.class,
        new BesuEventsImpl(
            besuController.getProtocolContext().getBlockchain(),
            besuController.getProtocolManager().getBlockBroadcaster(),
            besuController.getTransactionPool(),
            besuController.getSyncState()));
    besuPluginContext.startPlugins();

    final Runner runner =
        runnerBuilder
            .vertx(Vertx.vertx())
            .besuController(besuController)
            .ethNetworkConfig(ethNetworkConfig)
            .discovery(node.isDiscoveryEnabled())
            .p2pAdvertisedHost(node.getHostName())
            .p2pListenPort(0)
            .maxPeers(25)
            .networkingConfiguration(node.getNetworkingConfiguration())
            .jsonRpcConfiguration(node.jsonRpcConfiguration())
            .webSocketConfiguration(node.webSocketConfiguration())
            .dataDir(node.homeDirectory())
            .metricsSystem(metricsSystem)
            .permissioningService(new PermissioningServiceImpl())
            .metricsConfiguration(node.getMetricsConfiguration())
            .p2pEnabled(node.isP2pEnabled())
            .graphQLConfiguration(GraphQLConfiguration.createDefault())
            .staticNodes(
                node.getStaticNodes().stream()
                    .map(EnodeURLImpl::fromString)
                    .collect(Collectors.toList()))
            .besuPluginContext(new BesuPluginContextImpl())
            .autoLogBloomCaching(false)
            .storageProvider(storageProvider)
            .forkIdSupplier(() -> besuController.getProtocolManager().getForkIdAsBytesList())
            .build();

    runner.start();

    besuRunners.put(node.getName(), runner);
    ThreadContext.remove("node");
  }

  @Override
  public void stopNode(final BesuNode node) {
    final BesuPluginContextImpl pluginContext = besuPluginContextMap.remove(node);
    if (pluginContext != null) {
      pluginContext.stopPlugins();
    }
    node.stop();
    killRunner(node.getName());
  }

  @Override
  public void shutdown() {
    // stop all plugins from pluginContext
    besuPluginContextMap.values().forEach(BesuPluginContextImpl::stopPlugins);
    besuPluginContextMap.clear();

    // iterate over a copy of the set so that besuRunner can be updated when a runner is killed
    new HashSet<>(besuRunners.keySet()).forEach(this::killRunner);
  }

  @Override
  public boolean isActive(final String nodeName) {
    return besuRunners.containsKey(nodeName);
  }

  private void killRunner(final String name) {
    LOG.info("Killing " + name + " runner");

    if (besuRunners.containsKey(name)) {
      try {
        besuRunners.get(name).close();
        besuRunners.remove(name);
      } catch (final Exception e) {
        throw new RuntimeException("Error shutting down node " + name, e);
      }
    } else {
      LOG.error("There was a request to kill an unknown node: {}", name);
    }
  }

  @Override
  public void startConsoleCapture() {
    throw new RuntimeException("Console contents can only be captured in process execution");
  }

  @Override
  public String getConsoleContents() {
    throw new RuntimeException("Console contents can only be captured in process execution");
  }
}
