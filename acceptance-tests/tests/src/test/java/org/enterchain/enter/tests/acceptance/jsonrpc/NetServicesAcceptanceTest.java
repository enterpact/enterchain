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
package org.enterchain.enter.tests.acceptance.jsonrpc;

import org.enterchain.enter.tests.acceptance.dsl.AcceptanceTestBase;
import org.enterchain.enter.tests.acceptance.dsl.node.Node;
import org.enterchain.enter.tests.acceptance.dsl.node.cluster.Cluster;
import org.enterchain.enter.tests.acceptance.dsl.node.cluster.ClusterConfiguration;
import org.enterchain.enter.tests.acceptance.dsl.node.cluster.ClusterConfigurationBuilder;

import org.junit.After;
import org.junit.Test;

public class NetServicesAcceptanceTest extends AcceptanceTestBase {

  private Cluster noDiscoveryCluster;

  private Node nodeA;
  private Node nodeB;

  @Test
  public void shouldIndicateNetServicesEnabled() throws Exception {
    final ClusterConfiguration clusterConfiguration =
        new ClusterConfigurationBuilder().awaitPeerDiscovery(false).build();
    noDiscoveryCluster = new Cluster(clusterConfiguration, net);
    nodeA = besu.createArchiveNodeNetServicesEnabled("nodeA");
    nodeB = besu.createArchiveNodeNetServicesEnabled("nodeB");
    noDiscoveryCluster.start(nodeA, nodeB);

    nodeA.verify(net.netServicesAllActive());
    nodeB.verify(net.netServicesAllActive());
  }

  @Test
  public void shouldNotDisplayDisabledServices() throws Exception {
    final ClusterConfiguration clusterConfiguration =
        new ClusterConfigurationBuilder().awaitPeerDiscovery(false).build();
    noDiscoveryCluster = new Cluster(clusterConfiguration, net);
    nodeA = besu.createArchiveNodeNetServicesDisabled("nodeA");
    nodeB = besu.createArchiveNodeNetServicesDisabled("nodeB");
    noDiscoveryCluster.start(nodeA, nodeB);

    nodeA.verify(net.netServicesOnlyJsonRpcEnabled());
    nodeB.verify(net.netServicesOnlyJsonRpcEnabled());
  }

  @After
  public void closeDown() throws Exception {
    noDiscoveryCluster.close();
  }
}
