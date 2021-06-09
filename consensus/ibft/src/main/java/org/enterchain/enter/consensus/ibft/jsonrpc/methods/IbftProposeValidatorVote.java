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

import org.enterchain.enter.consensus.common.VoteProposer;
import org.enterchain.enter.consensus.common.VoteType;
import org.enterchain.enter.ethereum.api.jsonrpc.RpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.enterchain.enter.ethereum.core.Address;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IbftProposeValidatorVote implements JsonRpcMethod {
  private static final Logger LOG = LogManager.getLogger();
  private final VoteProposer voteProposer;

  public IbftProposeValidatorVote(final VoteProposer voteProposer) {
    this.voteProposer = voteProposer;
  }

  @Override
  public String getName() {
    return RpcMethod.IBFT_PROPOSE_VALIDATOR_VOTE.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {

    final Address validatorAddress = requestContext.getRequiredParameter(0, Address.class);
    final Boolean add = requestContext.getRequiredParameter(1, Boolean.class);
    LOG.trace(
        "Received RPC rpcName={} voteType={} address={}",
        getName(),
        add ? VoteType.ADD : VoteType.DROP,
        validatorAddress);

    if (add) {
      voteProposer.auth(validatorAddress);
    } else {
      voteProposer.drop(validatorAddress);
    }

    return new JsonRpcSuccessResponse(requestContext.getRequest().getId(), true);
  }
}
