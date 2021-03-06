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
package org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.priv;

import org.enterchain.enter.ethereum.api.jsonrpc.JsonRpcErrorConverter;
import org.enterchain.enter.ethereum.api.jsonrpc.RpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.AbstractBlockParameterMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.parameters.BlockParameter;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.parameters.JsonCallParameter;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.EnclavePublicKeyProvider;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.enterchain.enter.ethereum.api.query.BlockchainQueries;
import org.enterchain.enter.ethereum.privacy.PrivacyController;

public class PrivCall extends AbstractBlockParameterMethod {

  private final EnclavePublicKeyProvider enclavePublicKeyProvider;
  private final PrivacyController privacyController;

  public PrivCall(
      final BlockchainQueries blockchainQueries,
      final PrivacyController privacyController,
      final EnclavePublicKeyProvider enclavePublicKeyProvider) {
    super(blockchainQueries);
    this.enclavePublicKeyProvider = enclavePublicKeyProvider;
    this.privacyController = privacyController;
  }

  @Override
  public String getName() {
    return RpcMethod.PRIV_CALL.getMethodName();
  }

  @Override
  protected BlockParameter blockParameter(final JsonRpcRequestContext request) {
    return request.getRequiredParameter(2, BlockParameter.class);
  }

  @Override
  protected Object resultByBlockNumber(
      final JsonRpcRequestContext request, final long blockNumber) {
    final JsonCallParameter callParams = validateAndGetCallParams(request);
    final String privacyGroupId = request.getRequiredParameter(0, String.class);

    final String enclavePublicKey = enclavePublicKeyProvider.getEnclaveKey(request.getUser());

    PrivUtil.checkMembershipForAuthenticatedUser(
        privacyController, enclavePublicKeyProvider, request, privacyGroupId, blockNumber);

    return privacyController
        .simulatePrivateTransaction(privacyGroupId, enclavePublicKey, callParams, blockNumber)
        .map(
            result ->
                result
                    .getValidationResult()
                    .either(
                        (() ->
                            new JsonRpcSuccessResponse(
                                request.getRequest().getId(), result.getOutput().toString())),
                        reason ->
                            new JsonRpcErrorResponse(
                                request.getRequest().getId(),
                                JsonRpcErrorConverter.convertTransactionInvalidReason(reason))))
        .orElse(validRequestBlockNotFound(request));
  }

  private JsonRpcSuccessResponse validRequestBlockNotFound(final JsonRpcRequestContext request) {
    return new JsonRpcSuccessResponse(request.getRequest().getId(), null);
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    return (JsonRpcResponse) findResultByParamType(requestContext);
  }

  private JsonCallParameter validateAndGetCallParams(final JsonRpcRequestContext request) {
    final JsonCallParameter callParams = request.getRequiredParameter(1, JsonCallParameter.class);
    if (callParams.getTo() == null) {
      throw new InvalidJsonRpcParameters("Missing \"to\" field in call arguments");
    }
    return callParams;
  }
}
