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
package org.enterchain.enter.consensus.ibft.jsonrpc.methods;

import org.enterchain.enter.consensus.common.BlockInterface;
import org.enterchain.enter.ethereum.api.jsonrpc.RpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.Hash;

import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IbftGetValidatorsByBlockHash implements JsonRpcMethod {
  private static final Logger LOG = LogManager.getLogger();

  private final Blockchain blockchain;
  private final BlockInterface blockInterface;

  public IbftGetValidatorsByBlockHash(
      final Blockchain blockchain, final BlockInterface blockInterface) {
    this.blockchain = blockchain;
    this.blockInterface = blockInterface;
  }

  @Override
  public String getName() {
    return RpcMethod.IBFT_GET_VALIDATORS_BY_BLOCK_HASH.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    return new JsonRpcSuccessResponse(
        requestContext.getRequest().getId(), blockResult(requestContext));
  }

  private Object blockResult(final JsonRpcRequestContext request) {
    final Hash hash = request.getRequiredParameter(0, Hash.class);
    LOG.trace("Received RPC rpcName={} blockHash={}", getName(), hash);
    final Optional<BlockHeader> blockHeader = blockchain.getBlockHeader(hash);
    return blockHeader
        .map(
            header ->
                blockInterface.validatorsInBlock(header).stream()
                    .map(validator -> validator.toString())
                    .collect(Collectors.toList()))
        .orElse(null);
  }
}
