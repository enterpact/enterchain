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
import org.enterchain.enter.ethereum.api.jsonrpc.internal.filter.FilterManager;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.EnclavePublicKeyProvider;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.PrivGetFilterChanges;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.PrivGetFilterLogs;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.PrivUninstallFilter;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivCall;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivCreatePrivacyGroup;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivDebugGetStateRoot;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivDeletePrivacyGroup;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivDistributeRawTransaction;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivFindPrivacyGroup;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivGetCode;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivGetLogs;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivGetPrivacyPrecompileAddress;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivGetPrivateTransaction;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivGetTransactionCount;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivGetTransactionReceipt;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.priv.PrivNewFilter;
import org.enterchain.enter.ethereum.api.query.BlockchainQueries;
import org.enterchain.enter.ethereum.core.PrivacyParameters;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPool;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.privacy.PrivacyController;

import java.util.Map;

public class PrivJsonRpcMethods extends PrivacyApiGroupJsonRpcMethods {

  private final FilterManager filterManager;

  public PrivJsonRpcMethods(
      final BlockchainQueries blockchainQueries,
      final ProtocolSchedule protocolSchedule,
      final TransactionPool transactionPool,
      final PrivacyParameters privacyParameters,
      final FilterManager filterManager) {
    super(blockchainQueries, protocolSchedule, transactionPool, privacyParameters);
    this.filterManager = filterManager;
  }

  @Override
  protected RpcApi getApiGroup() {
    return RpcApis.PRIV;
  }

  @Override
  protected Map<String, JsonRpcMethod> create(
      final PrivacyController privacyController,
      final EnclavePublicKeyProvider enclavePublicKeyProvider) {

    final Map<String, JsonRpcMethod> RPC_METHODS =
        mapOf(
            new PrivCall(getBlockchainQueries(), privacyController, enclavePublicKeyProvider),
            new PrivDebugGetStateRoot(
                getBlockchainQueries(), enclavePublicKeyProvider, privacyController),
            new PrivDistributeRawTransaction(
                privacyController,
                enclavePublicKeyProvider,
                getPrivacyParameters().isOnchainPrivacyGroupsEnabled()),
            new PrivGetCode(getBlockchainQueries(), privacyController, enclavePublicKeyProvider),
            new PrivGetLogs(
                getBlockchainQueries(),
                getPrivacyQueries(),
                privacyController,
                enclavePublicKeyProvider),
            new PrivGetPrivateTransaction(privacyController, enclavePublicKeyProvider),
            new PrivGetPrivacyPrecompileAddress(getPrivacyParameters()),
            new PrivGetTransactionCount(privacyController, enclavePublicKeyProvider),
            new PrivGetTransactionReceipt(
                getPrivacyParameters().getPrivateStateStorage(),
                privacyController,
                enclavePublicKeyProvider),
            new PrivGetFilterLogs(filterManager, privacyController, enclavePublicKeyProvider),
            new PrivGetFilterChanges(filterManager, privacyController, enclavePublicKeyProvider),
            new PrivNewFilter(filterManager, privacyController, enclavePublicKeyProvider),
            new PrivUninstallFilter(filterManager, privacyController, enclavePublicKeyProvider));

    if (!getPrivacyParameters().isOnchainPrivacyGroupsEnabled()) {
      final Map<String, JsonRpcMethod> OFFCHAIN_METHODS =
          mapOf(
              new PrivCreatePrivacyGroup(privacyController, enclavePublicKeyProvider),
              new PrivDeletePrivacyGroup(privacyController, enclavePublicKeyProvider),
              new PrivFindPrivacyGroup(privacyController, enclavePublicKeyProvider));
      OFFCHAIN_METHODS.forEach(
          (key, jsonRpcMethod) ->
              RPC_METHODS.merge(key, jsonRpcMethod, (oldVal, newVal) -> newVal));
    }
    return RPC_METHODS;
  }
}
