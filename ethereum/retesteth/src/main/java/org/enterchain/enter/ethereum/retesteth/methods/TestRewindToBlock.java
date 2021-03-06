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
import org.enterchain.enter.ethereum.retesteth.RetestethContext;

public class TestRewindToBlock implements JsonRpcMethod {
  private final RetestethContext context;

  public TestRewindToBlock(final RetestethContext context) {
    this.context = context;
  }

  @Override
  public String getName() {
    return "test_rewindToBlock";
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    final long blockNumber = requestContext.getRequiredParameter(0, Long.TYPE);

    return new JsonRpcSuccessResponse(
        requestContext.getRequest().getId(), context.getBlockchain().rewindToBlock(blockNumber));
  }
}
