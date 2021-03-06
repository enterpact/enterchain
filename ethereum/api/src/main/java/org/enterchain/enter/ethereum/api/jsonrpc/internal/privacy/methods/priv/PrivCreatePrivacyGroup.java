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

import org.enterchain.enter.enclave.types.PrivacyGroup;
import org.enterchain.enter.ethereum.api.jsonrpc.JsonRpcEnclaveErrorConverter;
import org.enterchain.enter.ethereum.api.jsonrpc.RpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.EnclavePublicKeyProvider;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.parameters.CreatePrivacyGroupParameter;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.enterchain.enter.ethereum.privacy.PrivacyController;

import org.apache.logging.log4j.Logger;

public class PrivCreatePrivacyGroup implements JsonRpcMethod {

  private static final Logger LOG = getLogger();
  private final PrivacyController privacyController;
  private final EnclavePublicKeyProvider enclavePublicKeyProvider;

  public PrivCreatePrivacyGroup(
      final PrivacyController privacyController,
      final EnclavePublicKeyProvider enclavePublicKeyProvider) {
    this.privacyController = privacyController;
    this.enclavePublicKeyProvider = enclavePublicKeyProvider;
  }

  @Override
  public String getName() {
    return RpcMethod.PRIV_CREATE_PRIVACY_GROUP.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    LOG.trace("Executing {}", RpcMethod.PRIV_CREATE_PRIVACY_GROUP.getMethodName());

    final CreatePrivacyGroupParameter parameter =
        requestContext.getRequiredParameter(0, CreatePrivacyGroupParameter.class);

    LOG.trace(
        "Creating a privacy group with name {} and description {}",
        parameter.getName(),
        parameter.getDescription());

    final PrivacyGroup response;
    try {
      response =
          privacyController.createPrivacyGroup(
              parameter.getAddresses(),
              parameter.getName(),
              parameter.getDescription(),
              enclavePublicKeyProvider.getEnclaveKey(requestContext.getUser()));
    } catch (Exception e) {
      LOG.error("Failed to create privacy group", e);
      return new JsonRpcErrorResponse(
          requestContext.getRequest().getId(),
          JsonRpcEnclaveErrorConverter.convertEnclaveInvalidReason(e.getMessage()));
    }
    return new JsonRpcSuccessResponse(
        requestContext.getRequest().getId(), response.getPrivacyGroupId());
  }
}
