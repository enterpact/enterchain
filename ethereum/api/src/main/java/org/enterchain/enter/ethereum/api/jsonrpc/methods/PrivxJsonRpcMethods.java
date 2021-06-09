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
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.EnclavePublicKeyProvider;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.privx.PrivxFindOnChainPrivacyGroup;
import org.enterchain.enter.ethereum.api.query.BlockchainQueries;
import org.enterchain.enter.ethereum.core.PrivacyParameters;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPool;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.privacy.PrivacyController;

import java.util.Map;

public class PrivxJsonRpcMethods extends PrivacyApiGroupJsonRpcMethods {

  public PrivxJsonRpcMethods(
      final BlockchainQueries blockchainQueries,
      final ProtocolSchedule protocolSchedule,
      final TransactionPool transactionPool,
      final PrivacyParameters privacyParameters) {
    super(blockchainQueries, protocolSchedule, transactionPool, privacyParameters);
  }

  @Override
  protected RpcApi getApiGroup() {
    return RpcApis.PRIV;
  }

  @Override
  protected Map<String, JsonRpcMethod> create(
      final PrivacyController privacyController,
      final EnclavePublicKeyProvider enclavePublicKeyProvider) {
    if (getPrivacyParameters().isOnchainPrivacyGroupsEnabled()) {
      return mapOf(new PrivxFindOnChainPrivacyGroup(privacyController, enclavePublicKeyProvider));
    } else {
      return Map.of();
    }
  }
}
