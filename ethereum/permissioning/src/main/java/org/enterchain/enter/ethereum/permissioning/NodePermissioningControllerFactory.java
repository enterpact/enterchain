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
package org.enterchain.enter.ethereum.permissioning;

import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.Synchronizer;
import org.enterchain.enter.ethereum.p2p.peers.EnodeURLImpl;
import org.enterchain.enter.ethereum.permissioning.node.NodePermissioningController;
import org.enterchain.enter.ethereum.permissioning.node.provider.SyncStatusNodePermissioningProvider;
import org.enterchain.enter.ethereum.transaction.TransactionSimulator;
import org.enterchain.enter.plugin.data.EnodeURL;
import org.enterchain.enter.plugin.services.MetricsSystem;
import org.enterchain.enter.plugin.services.permissioning.NodeConnectionPermissioningProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

public class NodePermissioningControllerFactory {

  private static final Logger LOG = LogManager.getLogger();

  public NodePermissioningController create(
      final PermissioningConfiguration permissioningConfiguration,
      final Synchronizer synchronizer,
      final Collection<EnodeURL> fixedNodes,
      final Bytes localNodeId,
      final TransactionSimulator transactionSimulator,
      final MetricsSystem metricsSystem,
      final Blockchain blockchain,
      final List<NodeConnectionPermissioningProvider> pluginProviders) {

    ArrayList<NodeConnectionPermissioningProvider> providers = Lists.newArrayList(pluginProviders);

    final Optional<SyncStatusNodePermissioningProvider> syncStatusProviderOptional;

    if (permissioningConfiguration.getLocalConfig().isPresent()) {
      LocalPermissioningConfiguration localPermissioningConfiguration =
          permissioningConfiguration.getLocalConfig().get();
      if (localPermissioningConfiguration.isNodeAllowlistEnabled()) {
        NodeLocalConfigPermissioningController localProvider =
            new NodeLocalConfigPermissioningController(
                localPermissioningConfiguration,
                new ArrayList<>(fixedNodes),
                localNodeId,
                metricsSystem);
        providers.add(localProvider);
      }
    }

    if (permissioningConfiguration.getSmartContractConfig().isPresent()
        && permissioningConfiguration
            .getSmartContractConfig()
            .get()
            .isSmartContractNodeAllowlistEnabled()) {

      configureNodePermissioningSmartContractProvider(
          permissioningConfiguration, transactionSimulator, metricsSystem, providers);

      if (fixedNodes.isEmpty()) {
        syncStatusProviderOptional = Optional.empty();
      } else {
        syncStatusProviderOptional =
            Optional.of(
                new SyncStatusNodePermissioningProvider(synchronizer, fixedNodes, metricsSystem));
      }
    } else {
      syncStatusProviderOptional = Optional.empty();
    }

    final Optional<GoQuorumQip714Gate> goQuorumQip714Gate =
        permissioningConfiguration
            .getQuorumPermissioningConfig()
            .flatMap(
                config -> {
                  if (config.isEnabled()) {
                    return Optional.of(
                        GoQuorumQip714Gate.getInstance(config.getQip714Block(), blockchain));
                  } else {
                    return Optional.empty();
                  }
                });

    final NodePermissioningController nodePermissioningController =
        new NodePermissioningController(syncStatusProviderOptional, providers, goQuorumQip714Gate);

    permissioningConfiguration
        .getSmartContractConfig()
        .ifPresent(
            config -> {
              if (config.isSmartContractNodeAllowlistEnabled()) {
                validatePermissioningContract(
                    nodePermissioningController,
                    permissioningConfiguration.getSmartContractConfig().get());
              }
            });

    return nodePermissioningController;
  }

  private void configureNodePermissioningSmartContractProvider(
      final PermissioningConfiguration permissioningConfiguration,
      final TransactionSimulator transactionSimulator,
      final MetricsSystem metricsSystem,
      final List<NodeConnectionPermissioningProvider> providers) {
    final SmartContractPermissioningConfiguration smartContractPermissioningConfig =
        permissioningConfiguration.getSmartContractConfig().get();
    final Address nodePermissioningSmartContractAddress =
        smartContractPermissioningConfig.getNodeSmartContractAddress();

    final NodeConnectionPermissioningProvider smartContractProvider;
    switch (smartContractPermissioningConfig.getNodeSmartContractInterfaceVersion()) {
      case 1:
        {
          smartContractProvider =
              new NodeSmartContractPermissioningController(
                  nodePermissioningSmartContractAddress, transactionSimulator, metricsSystem);
          break;
        }
      case 2:
        {
          smartContractProvider =
              new NodeSmartContractV2PermissioningController(
                  nodePermissioningSmartContractAddress, transactionSimulator, metricsSystem);
          break;
        }
      default:
        throw new IllegalStateException(
            "Invalid node Smart contract permissioning interface version");
    }
    providers.add(smartContractProvider);
  }

  private void validatePermissioningContract(
      final NodePermissioningController nodePermissioningController,
      final SmartContractPermissioningConfiguration smartContractPermissioningConfig) {
    LOG.debug("Validating onchain node permissioning smart contract configuration");

    try {
      // the enodeURLs don't matter. We just want to check if a call to the smart contract succeeds
      nodePermissioningController.isPermitted(
          EnodeURLImpl.fromString(
              "enode://6f8a80d14311c39f35f516fa664deaaaa13e85b2f7493f37f6144d86991ec012937307647bd3b9a82abe2974e1407241d54947bbb39763a4cac9f77166ad92a0@10.3.58.6:30303"),
          EnodeURLImpl.fromString(
              "enode://6f8a80d14311c39f35f516fa664deaaaa13e85b2f7493f37f6144d86991ec012937307647bd3b9a82abe2974e1407241d54947bbb39763a4cac9f77166ad92a0@10.3.58.6:30303"));
    } catch (Exception e) {
      final String msg =
          String.format(
              "Error: node permissioning contract at address %s does not match the expected interface version %s.",
              smartContractPermissioningConfig.getNodeSmartContractAddress(),
              smartContractPermissioningConfig.getNodeSmartContractInterfaceVersion());
      throw new IllegalStateException(msg, e);
    }
  }
}
