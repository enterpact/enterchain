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

import static org.apache.logging.log4j.LogManager.getLogger;
import static org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcError.FIND_PRIVACY_GROUP_ERROR;

import org.enterchain.enter.enclave.types.PrivacyGroup;
import org.enterchain.enter.ethereum.api.jsonrpc.RpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.AbstractBlockParameterMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.parameters.BlockParameter;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.EnclavePublicKeyProvider;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcError;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.enterchain.enter.ethereum.api.query.BlockchainQueries;
import org.enterchain.enter.ethereum.privacy.MultiTenancyValidationException;
import org.enterchain.enter.ethereum.privacy.PrivacyController;

import java.util.Optional;

import org.apache.logging.log4j.Logger;

public class PrivDebugGetStateRoot extends AbstractBlockParameterMethod {

  private static final Logger LOG = getLogger();

  private final EnclavePublicKeyProvider enclavePublicKeyProvider;
  private final PrivacyController privacyController;

  public PrivDebugGetStateRoot(
      final BlockchainQueries blockchainQueries,
      final EnclavePublicKeyProvider enclavePublicKeyProvider,
      final PrivacyController privacyController) {
    super(blockchainQueries);
    this.enclavePublicKeyProvider = enclavePublicKeyProvider;
    this.privacyController = privacyController;
  }

  @Override
  public String getName() {
    return RpcMethod.PRIV_DEBUG_GET_STATE_ROOT.getMethodName();
  }

  @Override
  protected BlockParameter blockParameter(final JsonRpcRequestContext request) {
    return request.getRequiredParameter(1, BlockParameter.class);
  }

  @Override
  protected Object resultByBlockNumber(
      final JsonRpcRequestContext requestContext, final long blockNumber) {
    final String privacyGroupId = requestContext.getRequiredParameter(0, String.class);
    final String enclavePublicKey =
        enclavePublicKeyProvider.getEnclaveKey(requestContext.getUser());
    LOG.trace("Executing {}", this::getName);

    final Optional<PrivacyGroup> privacyGroup;
    try {
      privacyGroup = privacyController.findPrivacyGroupByGroupId(privacyGroupId, enclavePublicKey);
    } catch (final MultiTenancyValidationException e) {
      return new JsonRpcErrorResponse(
          requestContext.getRequest().getId(), FIND_PRIVACY_GROUP_ERROR);
    } catch (final Exception e) {
      return new JsonRpcErrorResponse(
          requestContext.getRequest().getId(), JsonRpcError.INVALID_PARAMS);
    }

    if (privacyGroup.isEmpty()) {
      LOG.error("Failed to retrieve privacy group");
      return new JsonRpcErrorResponse(
          requestContext.getRequest().getId(), FIND_PRIVACY_GROUP_ERROR);
    }

    return privacyController
        .getStateRootByBlockNumber(privacyGroupId, enclavePublicKey, blockNumber)
        .<JsonRpcResponse>map(
            stateRootHash ->
                new JsonRpcSuccessResponse(
                    requestContext.getRequest().getId(), stateRootHash.toString()))
        .orElse(
            new JsonRpcErrorResponse(
                requestContext.getRequest().getId(), JsonRpcError.INTERNAL_ERROR));
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    return (JsonRpcResponse) findResultByParamType(requestContext);
  }
}
