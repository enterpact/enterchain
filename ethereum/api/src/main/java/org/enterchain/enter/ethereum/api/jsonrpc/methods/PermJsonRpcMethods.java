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

import org.enterchain.enter.ethereum.api.jsonrpc.RpcApi;
import org.enterchain.enter.ethereum.api.jsonrpc.RpcApis;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.permissioning.PermAddAccountsToAllowlist;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.permissioning.PermAddAccountsToWhitelist;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.permissioning.PermAddNodesToAllowlist;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.permissioning.PermAddNodesToWhitelist;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.permissioning.PermGetAccountsAllowlist;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.permissioning.PermGetAccountsWhitelist;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.permissioning.PermGetNodesAllowlist;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.permissioning.PermGetNodesWhitelist;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.permissioning.PermReloadPermissionsFromFile;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.permissioning.PermRemoveAccountsFromAllowlist;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.permissioning.PermRemoveAccountsFromWhitelist;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.permissioning.PermRemoveNodesFromAllowlist;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.permissioning.PermRemoveNodesFromWhitelist;
import org.enterchain.enter.ethereum.permissioning.AccountLocalConfigPermissioningController;
import org.enterchain.enter.ethereum.permissioning.NodeLocalConfigPermissioningController;

import java.util.Map;
import java.util.Optional;

public class PermJsonRpcMethods extends ApiGroupJsonRpcMethods {

  private final Optional<AccountLocalConfigPermissioningController> accountsWhitelistController;
  private final Optional<NodeLocalConfigPermissioningController> nodeWhitelistController;

  public PermJsonRpcMethods(
      final Optional<AccountLocalConfigPermissioningController> accountsWhitelistController,
      final Optional<NodeLocalConfigPermissioningController> nodeWhitelistController) {
    this.accountsWhitelistController = accountsWhitelistController;
    this.nodeWhitelistController = nodeWhitelistController;
  }

  @Override
  protected RpcApi getApiGroup() {
    return RpcApis.PERM;
  }

  @Override
  protected Map<String, JsonRpcMethod> create() {
    return mapOf(
        new PermAddNodesToWhitelist(nodeWhitelistController),
        new PermAddNodesToAllowlist(nodeWhitelistController),
        new PermRemoveNodesFromWhitelist(nodeWhitelistController),
        new PermRemoveNodesFromAllowlist(nodeWhitelistController),
        new PermGetNodesWhitelist(nodeWhitelistController),
        new PermGetNodesAllowlist(nodeWhitelistController),
        new PermGetAccountsWhitelist(accountsWhitelistController),
        new PermGetAccountsAllowlist(accountsWhitelistController),
        new PermAddAccountsToWhitelist(accountsWhitelistController),
        new PermAddAccountsToAllowlist(accountsWhitelistController),
        new PermRemoveAccountsFromWhitelist(accountsWhitelistController),
        new PermRemoveAccountsFromAllowlist(accountsWhitelistController),
        new PermReloadPermissionsFromFile(accountsWhitelistController, nodeWhitelistController));
  }
}
