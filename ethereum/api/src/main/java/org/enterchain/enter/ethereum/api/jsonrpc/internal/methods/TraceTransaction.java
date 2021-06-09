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
import org.enterchain.enter.ethereum.api.jsonrpc.internal.processor.BlockTrace;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.processor.BlockTracer;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.processor.TransactionTrace;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.results.tracing.flat.FlatTraceGenerator;
import org.enterchain.enter.ethereum.api.query.BlockchainQueries;
import org.enterchain.enter.ethereum.api.query.TransactionWithMetadata;
import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.debug.TraceOptions;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.vm.DebugOperationTracer;

import java.util.Collections;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class TraceTransaction implements JsonRpcMethod {
  private final Supplier<BlockTracer> blockTracerSupplier;

  private final BlockchainQueries blockchainQueries;
  private final ProtocolSchedule protocolSchedule;

  public TraceTransaction(
      final Supplier<BlockTracer> blockTracerSupplier,
      final ProtocolSchedule protocolSchedule,
      final BlockchainQueries blockchainQueries) {
    this.blockTracerSupplier = blockTracerSupplier;
    this.blockchainQueries = blockchainQueries;
    this.protocolSchedule = protocolSchedule;
  }

  @Override
  public String getName() {
    return RpcMethod.TRACE_TRANSACTION.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    final Hash transactionHash = requestContext.getRequiredParameter(0, Hash.class);
    return new JsonRpcSuccessResponse(
        requestContext.getRequest().getId(), resultByTransactionHash(transactionHash));
  }

  private Object resultByTransactionHash(final Hash transactionHash) {
    return blockchainQueries
        .transactionByHash(transactionHash)
        .flatMap(TransactionWithMetadata::getBlockNumber)
        .flatMap(blockNumber -> blockchainQueries.getBlockchain().getBlockByNumber(blockNumber))
        .map((block) -> traceBlock(block, transactionHash))
        .orElse(emptyResult());
  }

  private Object traceBlock(final Block block, final Hash transactionHash) {
    if (block == null || block.getBody().getTransactions().isEmpty()) {
      return emptyResult();
    }
    final TransactionTrace transactionTrace =
        blockTracerSupplier.get()
            .trace(block, new DebugOperationTracer(new TraceOptions(false, false, true)))
            .map(BlockTrace::getTransactionTraces).orElse(Collections.emptyList()).stream()
            .filter(trxTrace -> trxTrace.getTransaction().getHash().equals(transactionHash))
            .findFirst()
            .orElseThrow();
    return generateTracesFromTransactionTraceAndBlock(protocolSchedule, transactionTrace, block);
  }

  private JsonNode generateTracesFromTransactionTraceAndBlock(
      final ProtocolSchedule protocolSchedule,
      final TransactionTrace transactionTrace,
      final Block block) {
    final ObjectMapper mapper = new ObjectMapper();

    final ArrayNode resultArrayNode = mapper.createArrayNode();

    FlatTraceGenerator.generateFromTransactionTraceAndBlock(
            protocolSchedule, transactionTrace, block)
        .forEachOrdered(resultArrayNode::addPOJO);

    return resultArrayNode;
  }

  private Object emptyResult() {
    final ObjectMapper mapper = new ObjectMapper();
    return mapper.createArrayNode();
  }
}
