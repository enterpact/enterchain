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

import org.enterchain.enter.enclave.EnclaveClientException;
import org.enterchain.enter.ethereum.api.jsonrpc.JsonRpcEnclaveErrorConverter;
import org.enterchain.enter.ethereum.api.jsonrpc.RpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.EnclavePublicKeyProvider;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.results.privacy.PrivateTransactionGroupResult;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.results.privacy.PrivateTransactionLegacyResult;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.results.privacy.PrivateTransactionResult;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.privacy.PrivacyController;
import org.enterchain.enter.ethereum.privacy.PrivateTransaction;

import java.util.Optional;

import org.apache.logging.log4j.Logger;

public class PrivGetPrivateTransaction implements JsonRpcMethod {

  private static final Logger LOG = getLogger();

  private final PrivacyController privacyController;
  private final EnclavePublicKeyProvider enclavePublicKeyProvider;

  public PrivGetPrivateTransaction(
      final PrivacyController privacyController,
      final EnclavePublicKeyProvider enclavePublicKeyProvider) {
    this.privacyController = privacyController;
    this.enclavePublicKeyProvider = enclavePublicKeyProvider;
  }

  @Override
  public String getName() {
    return RpcMethod.PRIV_GET_PRIVATE_TRANSACTION.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    LOG.trace("Executing {}", RpcMethod.PRIV_GET_PRIVATE_TRANSACTION.getMethodName());

    final Hash hash = requestContext.getRequiredParameter(0, Hash.class);
    final String enclaveKey = enclavePublicKeyProvider.getEnclaveKey(requestContext.getUser());

    final Optional<PrivateTransaction> maybePrivateTx;
    try {
      maybePrivateTx =
          privacyController
              .findPrivateTransactionByPmtHash(hash, enclaveKey)
              .map(PrivateTransaction.class::cast);
    } catch (EnclaveClientException e) {
      return new JsonRpcErrorResponse(
          requestContext.getRequest().getId(),
          JsonRpcEnclaveErrorConverter.convertEnclaveInvalidReason(e.getMessage()));
    }

    return maybePrivateTx
        .map(this::mapTransactionResult)
        .map(result -> new JsonRpcSuccessResponse(requestContext.getRequest().getId(), result))
        .orElse(new JsonRpcSuccessResponse(requestContext.getRequest().getId(), null));
  }

  private PrivateTransactionResult mapTransactionResult(
      final PrivateTransaction privateTransaction) {
    if (privateTransaction.getPrivacyGroupId().isPresent()) {
      return new PrivateTransactionGroupResult(privateTransaction);
    } else {
      return new PrivateTransactionLegacyResult(privateTransaction);
    }
  }
}
