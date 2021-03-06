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
import org.enterchain.enter.consensus.common.VoteTallyCache;
import org.enterchain.enter.consensus.common.jsonrpc.AbstractGetSignerMetricsMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.RpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.enterchain.enter.ethereum.api.query.BlockchainQueries;

public class IbftGetSignerMetrics extends AbstractGetSignerMetricsMethod implements JsonRpcMethod {

  public IbftGetSignerMetrics(
      final VoteTallyCache voteTallyCache,
      final BlockInterface blockInterface,
      final BlockchainQueries blockchainQueries) {
    super(voteTallyCache, blockInterface, blockchainQueries);
  }

  @Override
  public String getName() {
    return RpcMethod.IBFT_GET_SIGNER_METRICS.getMethodName();
  }
}
