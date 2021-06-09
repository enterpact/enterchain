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
package org.enterchain.enter.ethereum.retesteth;

import org.enterchain.enter.ethereum.api.jsonrpc.JsonRpcConfiguration;
import org.enterchain.enter.ethereum.api.jsonrpc.JsonRpcHttpService;
import org.enterchain.enter.ethereum.api.jsonrpc.health.HealthService;
import org.enterchain.enter.ethereum.api.jsonrpc.health.LivenessCheck;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.DebugAccountRange;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.DebugStorageRangeAt;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.EthBlockNumber;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.EthGetBalance;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.EthGetBlockByHash;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.EthGetBlockByNumber;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.EthGetCode;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.EthGetTransactionCount;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.EthSendRawTransaction;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.Web3ClientVersion;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.results.BlockResultFactory;
import org.enterchain.enter.ethereum.retesteth.methods.TestGetLogHash;
import org.enterchain.enter.ethereum.retesteth.methods.TestImportRawBlock;
import org.enterchain.enter.ethereum.retesteth.methods.TestMineBlocks;
import org.enterchain.enter.ethereum.retesteth.methods.TestModifyTimestamp;
import org.enterchain.enter.ethereum.retesteth.methods.TestRewindToBlock;
import org.enterchain.enter.ethereum.retesteth.methods.TestSetChainParams;
import org.enterchain.enter.metrics.noop.NoOpMetricsSystem;
import org.enterchain.enter.nat.NatService;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.vertx.core.Vertx;

public class RetestethService {

  private final JsonRpcHttpService jsonRpcHttpService;
  private final Vertx vertx;

  private final RetestethContext retestethContext;

  public RetestethService(
      final String clientVersion,
      final RetestethConfiguration retestethConfiguration,
      final JsonRpcConfiguration jsonRpcConfiguration) {
    vertx = Vertx.vertx();
    retestethContext = new RetestethContext();

    final BlockResultFactory blockResult = new BlockResultFactory();
    final NatService natService = new NatService(Optional.empty());

    final Map<String, JsonRpcMethod> jsonRpcMethods =
        mapOf(
            new Web3ClientVersion(clientVersion),
            new TestSetChainParams(retestethContext),
            new TestImportRawBlock(retestethContext),
            new EthBlockNumber(retestethContext::getBlockchainQueries, true),
            new EthGetBlockByNumber(retestethContext::getBlockchainQueries, blockResult, true),
            new DebugAccountRange(retestethContext::getBlockchainQueries),
            new EthGetBalance(retestethContext::getBlockchainQueries),
            new EthGetBlockByHash(retestethContext::getBlockchainQueries, blockResult, true),
            new EthGetCode(retestethContext::getBlockchainQueries),
            new EthGetTransactionCount(
                retestethContext::getBlockchainQueries,
                retestethContext::getPendingTransactions,
                false),
            new DebugStorageRangeAt(
                retestethContext::getBlockchainQueries, retestethContext::getBlockReplay, true),
            new TestModifyTimestamp(retestethContext),
            new EthSendRawTransaction(retestethContext::getTransactionPool, true),
            new TestMineBlocks(retestethContext),
            new TestGetLogHash(retestethContext),
            new TestRewindToBlock(retestethContext));

    jsonRpcHttpService =
        new JsonRpcHttpService(
            vertx,
            retestethConfiguration.getDataPath(),
            jsonRpcConfiguration,
            new NoOpMetricsSystem(),
            natService,
            jsonRpcMethods,
            new HealthService(new LivenessCheck()),
            HealthService.ALWAYS_HEALTHY);
  }

  public void start() {
    jsonRpcHttpService.start();
  }

  public void close() {
    stop();
  }

  public void stop() {
    jsonRpcHttpService.stop();
    vertx.close();
  }

  private static Map<String, JsonRpcMethod> mapOf(final JsonRpcMethod... rpcMethods) {
    return Arrays.stream(rpcMethods)
        .collect(Collectors.toMap(JsonRpcMethod::getName, rpcMethod -> rpcMethod));
  }
}
