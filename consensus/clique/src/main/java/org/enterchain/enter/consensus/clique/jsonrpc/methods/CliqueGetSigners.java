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
package org.enterchain.enter.consensus.clique.jsonrpc.methods;

import org.enterchain.enter.consensus.common.VoteTallyCache;
import org.enterchain.enter.ethereum.api.jsonrpc.RpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.parameters.BlockParameter;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcError;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.enterchain.enter.ethereum.api.query.BlockWithMetadata;
import org.enterchain.enter.ethereum.api.query.BlockchainQueries;
import org.enterchain.enter.ethereum.core.BlockHeader;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class CliqueGetSigners implements JsonRpcMethod {
  private final BlockchainQueries blockchainQueries;
  private final VoteTallyCache voteTallyCache;

  public CliqueGetSigners(
      final BlockchainQueries blockchainQueries, final VoteTallyCache voteTallyCache) {
    this.blockchainQueries = blockchainQueries;
    this.voteTallyCache = voteTallyCache;
  }

  @Override
  public String getName() {
    return RpcMethod.CLIQUE_GET_SIGNERS.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    final Optional<BlockHeader> blockHeader = determineBlockHeader(requestContext);
    return blockHeader
        .map(bh -> voteTallyCache.getVoteTallyAfterBlock(bh).getValidators())
        .map(addresses -> addresses.stream().map(Objects::toString).collect(Collectors.toList()))
        .<JsonRpcResponse>map(
            addresses -> new JsonRpcSuccessResponse(requestContext.getRequest().getId(), addresses))
        .orElse(
            new JsonRpcErrorResponse(
                requestContext.getRequest().getId(), JsonRpcError.INTERNAL_ERROR));
  }

  private Optional<BlockHeader> determineBlockHeader(final JsonRpcRequestContext request) {
    final Optional<BlockParameter> blockParameter =
        request.getOptionalParameter(0, BlockParameter.class);
    final long latest = blockchainQueries.headBlockNumber();
    final long blockNumber = blockParameter.map(b -> b.getNumber().orElse(latest)).orElse(latest);
    return blockchainQueries.blockByNumber(blockNumber).map(BlockWithMetadata::getHeader);
  }
}
