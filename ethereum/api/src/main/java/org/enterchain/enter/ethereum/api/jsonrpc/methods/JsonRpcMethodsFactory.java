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
package org.enterchain.enter.ethereum.api.jsonrpc.methods;

import org.enterchain.enter.config.GenesisConfigOptions;
import org.enterchain.enter.ethereum.api.jsonrpc.JsonRpcConfiguration;
import org.enterchain.enter.ethereum.api.jsonrpc.RpcApi;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.filter.FilterManager;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.RpcModules;
import org.enterchain.enter.ethereum.api.jsonrpc.websocket.WebSocketConfiguration;
import org.enterchain.enter.ethereum.api.query.BlockchainQueries;
import org.enterchain.enter.ethereum.blockcreation.MiningCoordinator;
import org.enterchain.enter.ethereum.core.PrivacyParameters;
import org.enterchain.enter.ethereum.core.Synchronizer;
import org.enterchain.enter.ethereum.eth.manager.EthPeers;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPool;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.p2p.network.P2PNetwork;
import org.enterchain.enter.ethereum.p2p.rlpx.wire.Capability;
import org.enterchain.enter.ethereum.permissioning.AccountLocalConfigPermissioningController;
import org.enterchain.enter.ethereum.permissioning.NodeLocalConfigPermissioningController;
import org.enterchain.enter.metrics.ObservableMetricsSystem;
import org.enterchain.enter.metrics.prometheus.MetricsConfiguration;
import org.enterchain.enter.nat.NatService;
import org.enterchain.enter.plugin.BesuPlugin;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class JsonRpcMethodsFactory {

  public Map<String, JsonRpcMethod> methods(
      final String clientVersion,
      final BigInteger networkId,
      final GenesisConfigOptions genesisConfigOptions,
      final P2PNetwork p2pNetwork,
      final BlockchainQueries blockchainQueries,
      final Synchronizer synchronizer,
      final ProtocolSchedule protocolSchedule,
      final FilterManager filterManager,
      final TransactionPool transactionPool,
      final MiningCoordinator miningCoordinator,
      final ObservableMetricsSystem metricsSystem,
      final Set<Capability> supportedCapabilities,
      final Optional<AccountLocalConfigPermissioningController> accountsWhitelistController,
      final Optional<NodeLocalConfigPermissioningController> nodeWhitelistController,
      final Collection<RpcApi> rpcApis,
      final PrivacyParameters privacyParameters,
      final JsonRpcConfiguration jsonRpcConfiguration,
      final WebSocketConfiguration webSocketConfiguration,
      final MetricsConfiguration metricsConfiguration,
      final NatService natService,
      final Map<String, BesuPlugin> namedPlugins,
      final Path dataDir,
      final EthPeers ethPeers) {
    final Map<String, JsonRpcMethod> enabled = new HashMap<>();

    if (!rpcApis.isEmpty()) {
      final JsonRpcMethod modules = new RpcModules(rpcApis);
      enabled.put(modules.getName(), modules);

      final List<JsonRpcMethods> availableApiGroups =
          List.of(
              new AdminJsonRpcMethods(
                  clientVersion,
                  networkId,
                  genesisConfigOptions,
                  p2pNetwork,
                  blockchainQueries,
                  namedPlugins,
                  natService,
                  ethPeers),
              new DebugJsonRpcMethods(
                  blockchainQueries, protocolSchedule, metricsSystem, transactionPool, dataDir),
              new EeaJsonRpcMethods(
                  blockchainQueries, protocolSchedule, transactionPool, privacyParameters),
              new GoQuorumJsonRpcPrivacyMethods(
                  blockchainQueries, protocolSchedule, transactionPool, privacyParameters),
              new EthJsonRpcMethods(
                  blockchainQueries,
                  synchronizer,
                  protocolSchedule,
                  filterManager,
                  transactionPool,
                  miningCoordinator,
                  supportedCapabilities,
                  privacyParameters),
              new NetJsonRpcMethods(
                  p2pNetwork,
                  networkId,
                  jsonRpcConfiguration,
                  webSocketConfiguration,
                  metricsConfiguration),
              new MinerJsonRpcMethods(miningCoordinator),
              new PermJsonRpcMethods(accountsWhitelistController, nodeWhitelistController),
              new PrivJsonRpcMethods(
                  blockchainQueries,
                  protocolSchedule,
                  transactionPool,
                  privacyParameters,
                  filterManager),
              new PrivxJsonRpcMethods(
                  blockchainQueries, protocolSchedule, transactionPool, privacyParameters),
              new Web3JsonRpcMethods(clientVersion),
              // TRACE Methods (Disabled while under development)
              new TraceJsonRpcMethods(blockchainQueries, protocolSchedule),
              new TxPoolJsonRpcMethods(transactionPool),
              new PluginsJsonRpcMethods(namedPlugins));

      for (final JsonRpcMethods apiGroup : availableApiGroups) {
        enabled.putAll(apiGroup.create(rpcApis));
      }
    }

    return enabled;
  }
}
