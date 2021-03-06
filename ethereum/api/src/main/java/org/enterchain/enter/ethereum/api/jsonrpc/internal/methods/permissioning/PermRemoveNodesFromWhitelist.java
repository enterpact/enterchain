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
import org.enterchain.enter.ethereum.permissioning.NodeLocalConfigPermissioningController;

import java.util.Optional;

@Deprecated
public class PermRemoveNodesFromWhitelist extends PermRemoveNodesFromAllowlist {

  public PermRemoveNodesFromWhitelist(
      final Optional<NodeLocalConfigPermissioningController> nodeAllowlistPermissioningController) {
    super(nodeAllowlistPermissioningController);
  }

  @Override
  public String getName() {
    return RpcMethod.PERM_REMOVE_NODES_FROM_WHITELIST.getMethodName();
  }
}
