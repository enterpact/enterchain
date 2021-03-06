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
package org.enterchain.enter.ethereum.retesteth.methods;

import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcError;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.BlockImporter;
import org.enterchain.enter.ethereum.retesteth.RetestethContext;
import org.enterchain.enter.ethereum.rlp.RLP;
import org.enterchain.enter.ethereum.rlp.RLPException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

public class TestImportRawBlock implements JsonRpcMethod {
  private static final Logger LOG = LogManager.getLogger();

  public static final String METHOD_NAME = "test_importRawBlock";

  private final RetestethContext context;

  public TestImportRawBlock(final RetestethContext context) {
    this.context = context;
  }

  @Override
  public String getName() {
    return METHOD_NAME;
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    final String input = requestContext.getRequiredParameter(0, String.class);
    final ProtocolContext protocolContext = this.context.getProtocolContext();

    final Block block;
    try {
      block =
          Block.readFrom(RLP.input(Bytes.fromHexString(input)), context.getBlockHeaderFunctions());
    } catch (final RLPException | IllegalArgumentException e) {
      LOG.debug("Failed to parse block RLP", e);
      return new JsonRpcErrorResponse(
          requestContext.getRequest().getId(), JsonRpcError.BLOCK_RLP_IMPORT_ERROR);
    }

    final BlockImporter blockImporter =
        context.getProtocolSpec(block.getHeader().getNumber()).getBlockImporter();
    if (blockImporter.importBlock(
        protocolContext,
        block,
        context.getHeaderValidationMode(),
        context.getHeaderValidationMode())) {
      return new JsonRpcSuccessResponse(
          requestContext.getRequest().getId(), block.getHash().toString());
    } else {
      LOG.debug("Failed to import block.");
      return new JsonRpcErrorResponse(
          requestContext.getRequest().getId(), JsonRpcError.BLOCK_IMPORT_ERROR);
    }
  }
}
