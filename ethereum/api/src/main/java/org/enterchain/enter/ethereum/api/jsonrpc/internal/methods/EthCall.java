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

import static org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcError.BLOCK_NOT_FOUND;

import org.enterchain.enter.ethereum.api.jsonrpc.JsonRpcErrorConverter;
import org.enterchain.enter.ethereum.api.jsonrpc.RpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.parameters.BlockParameterOrBlockHash;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.parameters.JsonCallParameter;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcError;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.enterchain.enter.ethereum.api.query.BlockchainQueries;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.mainnet.ImmutableTransactionValidationParams;
import org.enterchain.enter.ethereum.mainnet.TransactionValidationParams;
import org.enterchain.enter.ethereum.mainnet.ValidationResult;
import org.enterchain.enter.ethereum.processing.TransactionProcessingResult;
import org.enterchain.enter.ethereum.transaction.TransactionInvalidReason;
import org.enterchain.enter.ethereum.transaction.TransactionSimulator;
import org.enterchain.enter.ethereum.transaction.TransactionSimulatorResult;
import org.enterchain.enter.ethereum.vm.OperationTracer;

public class EthCall extends AbstractBlockParameterOrBlockHashMethod {
  private final TransactionSimulator transactionSimulator;

  public EthCall(
      final BlockchainQueries blockchainQueries, final TransactionSimulator transactionSimulator) {
    super(blockchainQueries);
    this.transactionSimulator = transactionSimulator;
  }

  @Override
  public String getName() {
    return RpcMethod.ETH_CALL.getMethodName();
  }

  @Override
  protected BlockParameterOrBlockHash blockParameterOrBlockHash(
      final JsonRpcRequestContext request) {
    return request.getRequiredParameter(1, BlockParameterOrBlockHash.class);
  }

  @Override
  protected Object resultByBlockHash(final JsonRpcRequestContext request, final Hash blockHash) {
    final JsonCallParameter callParams = validateAndGetCallParams(request);
    final BlockHeader header = blockchainQueries.get().getBlockHeaderByHash(blockHash).orElse(null);

    return transactionSimulator
        .process(
            callParams,
            ImmutableTransactionValidationParams.builder()
                .from(TransactionValidationParams.transactionSimulator())
                .isAllowExceedingBalance(!callParams.isStrict())
                .build(),
            OperationTracer.NO_TRACING,
            header)
        .map(
            result ->
                result
                    .getValidationResult()
                    .either(
                        (() ->
                            result.isSuccessful()
                                ? new JsonRpcSuccessResponse(
                                    request.getRequest().getId(), result.getOutput().toString())
                                : errorResponse(request, result)),
                        reason ->
                            new JsonRpcErrorResponse(
                                request.getRequest().getId(),
                                JsonRpcErrorConverter.convertTransactionInvalidReason(reason))))
        .orElse(errorResponse(request, BLOCK_NOT_FOUND));
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    return (JsonRpcResponse) handleParamTypes(requestContext);
  }

  private JsonRpcErrorResponse errorResponse(
      final JsonRpcRequestContext request, final TransactionSimulatorResult result) {
    final JsonRpcError jsonRpcError;

    final ValidationResult<TransactionInvalidReason> validationResult =
        result.getValidationResult();
    if (validationResult != null && !validationResult.isValid()) {
      jsonRpcError =
          JsonRpcErrorConverter.convertTransactionInvalidReason(
              validationResult.getInvalidReason());
    } else {
      final TransactionProcessingResult resultTrx = result.getResult();
      if (resultTrx != null && resultTrx.getRevertReason().isPresent()) {
        jsonRpcError = JsonRpcError.REVERT_ERROR;
        jsonRpcError.setData(resultTrx.getRevertReason().get().toHexString());
      } else {
        jsonRpcError = JsonRpcError.INTERNAL_ERROR;
      }
    }
    return errorResponse(request, jsonRpcError);
  }

  private JsonRpcErrorResponse errorResponse(
      final JsonRpcRequestContext request, final JsonRpcError jsonRpcError) {
    return new JsonRpcErrorResponse(request.getRequest().getId(), jsonRpcError);
  }

  private JsonCallParameter validateAndGetCallParams(final JsonRpcRequestContext request) {
    final JsonCallParameter callParams = request.getRequiredParameter(0, JsonCallParameter.class);
    if (callParams.getTo() == null) {
      throw new InvalidJsonRpcParameters("Missing \"to\" field in call arguments");
    }
    if (callParams.getGasPrice() != null
        && (callParams.getMaxFeePerGas().isPresent()
            || callParams.getMaxPriorityFeePerGas().isPresent())) {
      throw new InvalidJsonRpcParameters(
          "gasPrice cannot be used with maxFeePerGas or maxPriorityFeePerGas");
    }
    return callParams;
  }
}
