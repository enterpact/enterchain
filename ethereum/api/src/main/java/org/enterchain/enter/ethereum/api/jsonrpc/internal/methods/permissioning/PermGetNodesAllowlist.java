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
package org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.permissioning;

import org.enterchain.enter.ethereum.api.jsonrpc.RpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcError;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.enterchain.enter.ethereum.p2p.network.exceptions.P2PDisabledException;
import org.enterchain.enter.ethereum.permissioning.NodeLocalConfigPermissioningController;

import java.util.List;
import java.util.Optional;

public class PermGetNodesAllowlist implements JsonRpcMethod {

  private final Optional<NodeLocalConfigPermissioningController>
      nodeWhitelistPermissioningController;

  public PermGetNodesAllowlist(
      final Optional<NodeLocalConfigPermissioningController> nodeWhitelistPermissioningController) {
    this.nodeWhitelistPermissioningController = nodeWhitelistPermissioningController;
  }

  @Override
  public String getName() {
    return RpcMethod.PERM_GET_NODES_ALLOWLIST.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    try {
      if (nodeWhitelistPermissioningController.isPresent()) {
        final List<String> enodeList =
            nodeWhitelistPermissioningController.get().getNodesAllowlist();

        return new JsonRpcSuccessResponse(requestContext.getRequest().getId(), enodeList);
      } else {
        return new JsonRpcErrorResponse(
            requestContext.getRequest().getId(), JsonRpcError.NODE_ALLOWLIST_NOT_ENABLED);
      }
    } catch (P2PDisabledException e) {
      return new JsonRpcErrorResponse(
          requestContext.getRequest().getId(), JsonRpcError.P2P_DISABLED);
    }
  }
}
