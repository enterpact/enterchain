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
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.DebugAccountAt;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.DebugAccountRange;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.DebugBatchSendRawTransaction;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.DebugGetBadBlocks;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.DebugMetrics;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.DebugStandardTraceBadBlockToFile;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.DebugStandardTraceBlockToFile;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.DebugStorageRangeAt;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.DebugTraceBlock;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.DebugTraceBlockByHash;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.DebugTraceBlockByNumber;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.DebugTraceTransaction;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.processor.BlockReplay;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.processor.BlockTracer;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.processor.TransactionTracer;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.results.BlockResultFactory;
import org.enterchain.enter.ethereum.api.query.BlockchainQueries;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPool;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.mainnet.ScheduleBasedBlockHeaderFunctions;
import org.enterchain.enter.metrics.ObservableMetricsSystem;

import java.nio.file.Path;
import java.util.Map;

public class DebugJsonRpcMethods extends ApiGroupJsonRpcMethods {

  private final BlockResultFactory blockResult = new BlockResultFactory();

  private final BlockchainQueries blockchainQueries;
  private final ProtocolSchedule protocolSchedule;
  private final ObservableMetricsSystem metricsSystem;
  private final TransactionPool transactionPool;
  private final Path dataDir;

  DebugJsonRpcMethods(
      final BlockchainQueries blockchainQueries,
      final ProtocolSchedule protocolSchedule,
      final ObservableMetricsSystem metricsSystem,
      final TransactionPool transactionPool,
      final Path dataDir) {
    this.blockchainQueries = blockchainQueries;
    this.protocolSchedule = protocolSchedule;
    this.metricsSystem = metricsSystem;
    this.transactionPool = transactionPool;
    this.dataDir = dataDir;
  }

  @Override
  protected RpcApi getApiGroup() {
    return RpcApis.DEBUG;
  }

  @Override
  protected Map<String, JsonRpcMethod> create() {
    final BlockReplay blockReplay =
        new BlockReplay(
            protocolSchedule,
            blockchainQueries.getBlockchain(),
            blockchainQueries.getWorldStateArchive());

    return mapOf(
        new DebugTraceTransaction(blockchainQueries, new TransactionTracer(blockReplay)),
        new DebugAccountRange(blockchainQueries),
        new DebugStorageRangeAt(blockchainQueries, blockReplay),
        new DebugMetrics(metricsSystem),
        new DebugTraceBlock(
            () -> new BlockTracer(blockReplay),
            ScheduleBasedBlockHeaderFunctions.create(protocolSchedule),
            blockchainQueries),
        new DebugTraceBlockByNumber(() -> new BlockTracer(blockReplay), blockchainQueries),
        new DebugTraceBlockByHash(() -> new BlockTracer(blockReplay)),
        new DebugBatchSendRawTransaction(transactionPool),
        new DebugGetBadBlocks(blockchainQueries, protocolSchedule, blockResult),
        new DebugStandardTraceBlockToFile(
            () -> new TransactionTracer(blockReplay), blockchainQueries, dataDir),
        new DebugStandardTraceBadBlockToFile(
            () -> new TransactionTracer(blockReplay), blockchainQueries, protocolSchedule, dataDir),
        new DebugAccountAt(blockchainQueries, () -> new BlockTracer(blockReplay)));
  }
}
