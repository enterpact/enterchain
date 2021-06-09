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
package org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods;

import org.enterchain.enter.ethereum.api.jsonrpc.RpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.filter.FilterManager;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcError;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.results.LogsResult;
import org.enterchain.enter.ethereum.core.LogWithMetadata;
import org.enterchain.enter.ethereum.privacy.PrivacyController;

import java.util.List;

public class PrivGetFilterLogs implements JsonRpcMethod {

  private final PrivacyController privacyController;
  private final EnclavePublicKeyProvider enclavePublicKeyProvider;
  private final FilterManager filterManager;

  public PrivGetFilterLogs(
      final FilterManager filterManager,
      final PrivacyController privacyController,
      final EnclavePublicKeyProvider enclavePublicKeyProvider) {
    this.filterManager = filterManager;
    this.privacyController = privacyController;
    this.enclavePublicKeyProvider = enclavePublicKeyProvider;
  }

  @Override
  public String getName() {
    return RpcMethod.PRIV_GET_FILTER_LOGS.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext request) {
    final String privacyGroupId = request.getRequiredParameter(0, String.class);
    final String filterId = request.getRequiredParameter(1, String.class);

    checkIfPrivacyGroupMatchesAuthenticatedEnclaveKey(request, privacyGroupId);

    final List<LogWithMetadata> logs = filterManager.logs(filterId);
    if (logs != null) {
      return new JsonRpcSuccessResponse(request.getRequest().getId(), new LogsResult(logs));
    }

    return new JsonRpcErrorResponse(
        request.getRequest().getId(), JsonRpcError.LOGS_FILTER_NOT_FOUND);
  }

  private void checkIfPrivacyGroupMatchesAuthenticatedEnclaveKey(
      final JsonRpcRequestContext request, final String privacyGroupId) {
    final String enclavePublicKey = enclavePublicKeyProvider.getEnclaveKey(request.getUser());
    privacyController.verifyPrivacyGroupContainsEnclavePublicKey(privacyGroupId, enclavePublicKey);
  }
}
