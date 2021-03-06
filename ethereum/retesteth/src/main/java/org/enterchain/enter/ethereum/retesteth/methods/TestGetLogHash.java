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

import org.enterchain.enter.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.enterchain.enter.ethereum.api.query.TransactionReceiptWithMetadata;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.core.Log;
import org.enterchain.enter.ethereum.retesteth.RetestethContext;
import org.enterchain.enter.ethereum.rlp.RLP;

import java.util.Optional;

public class TestGetLogHash implements JsonRpcMethod {
  private final RetestethContext context;

  public TestGetLogHash(final RetestethContext context) {
    this.context = context;
  }

  @Override
  public String getName() {
    return "test_getLogHash";
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    final Hash txHash = requestContext.getRequiredParameter(0, Hash.class);

    final Optional<TransactionReceiptWithMetadata> receipt =
        context.getBlockchainQueries().transactionReceiptByTransactionHash(txHash);
    return new JsonRpcSuccessResponse(
        requestContext.getRequest().getId(),
        receipt.map(this::calculateLogHash).orElse(Hash.EMPTY_LIST_HASH).toString());
  }

  private Hash calculateLogHash(
      final TransactionReceiptWithMetadata transactionReceiptWithMetadata) {
    return Hash.hash(
        RLP.encode(
            out ->
                out.writeList(
                    transactionReceiptWithMetadata.getReceipt().getLogs(), Log::writeTo)));
  }
}
