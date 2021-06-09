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
package org.enterchain.enter.ethereum.api.jsonrpc.internal.methods;

import org.enterchain.enter.ethereum.api.jsonrpc.RpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.enterchain.enter.ethereum.p2p.network.P2PNetwork;
import org.enterchain.enter.ethereum.p2p.peers.DefaultPeer;
import org.enterchain.enter.ethereum.p2p.peers.EnodeURLImpl;
import org.enterchain.enter.ethereum.p2p.peers.Peer;
import org.enterchain.enter.plugin.data.EnodeURL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdminAddPeer extends AdminModifyPeer {

  private static final Logger LOG = LogManager.getLogger();

  public AdminAddPeer(final P2PNetwork peerNetwork) {
    super(peerNetwork);
  }

  @Override
  public String getName() {
    return RpcMethod.ADMIN_ADD_PEER.getMethodName();
  }

  @Override
  protected JsonRpcResponse performOperation(final Object id, final String enode) {
    LOG.debug("Adding ({}) to peers", enode);
    final EnodeURL enodeURL = EnodeURLImpl.fromString(enode);
    final Peer peer = DefaultPeer.fromEnodeURL(enodeURL);
    final boolean addedToNetwork = peerNetwork.addMaintainConnectionPeer(peer);
    return new JsonRpcSuccessResponse(id, addedToNetwork);
  }
}
