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
import org.enterchain.enter.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.parameters.TransactionTraceParams;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.processor.BlockTrace;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.processor.BlockTracer;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.results.DebugTraceTransactionResult;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.debug.TraceOptions;
import org.enterchain.enter.ethereum.vm.DebugOperationTracer;

import java.util.Collection;
import java.util.function.Supplier;

public class DebugTraceBlockByHash implements JsonRpcMethod {

  private final Supplier<BlockTracer> blockTracerSupplier;

  public DebugTraceBlockByHash(final Supplier<BlockTracer> blockTracerSupplier) {
    this.blockTracerSupplier = blockTracerSupplier;
  }

  @Override
  public String getName() {
    return RpcMethod.DEBUG_TRACE_BLOCK_BY_HASH.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    final Hash blockHash = requestContext.getRequiredParameter(0, Hash.class);
    final TraceOptions traceOptions =
        requestContext
            .getOptionalParameter(1, TransactionTraceParams.class)
            .map(TransactionTraceParams::traceOptions)
            .orElse(TraceOptions.DEFAULT);

    final Collection<DebugTraceTransactionResult> results =
        blockTracerSupplier
            .get()
            .trace(blockHash, new DebugOperationTracer(traceOptions))
            .map(BlockTrace::getTransactionTraces)
            .map(DebugTraceTransactionResult::of)
            .orElse(null);
    return new JsonRpcSuccessResponse(requestContext.getRequest().getId(), results);
  }
}
