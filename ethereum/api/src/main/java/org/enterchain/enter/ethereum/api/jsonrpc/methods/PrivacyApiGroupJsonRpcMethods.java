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

import org.enterchain.enter.ethereum.api.jsonrpc.LatestNonceProvider;
import org.enterchain.enter.ethereum.api.jsonrpc.RpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.DisabledPrivacyRpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.EnclavePublicKeyProvider;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.MultiTenancyRpcMethodDecorator;
import org.enterchain.enter.ethereum.api.query.BlockchainQueries;
import org.enterchain.enter.ethereum.api.query.PrivacyQueries;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.PrivacyParameters;
import org.enterchain.enter.ethereum.eth.transactions.PendingTransactions;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPool;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.privacy.ChainHeadPrivateNonceProvider;
import org.enterchain.enter.ethereum.privacy.DefaultPrivacyController;
import org.enterchain.enter.ethereum.privacy.MultiTenancyPrivacyController;
import org.enterchain.enter.ethereum.privacy.PrivacyController;
import org.enterchain.enter.ethereum.privacy.PrivateNonceProvider;
import org.enterchain.enter.ethereum.privacy.PrivateTransactionSimulator;
import org.enterchain.enter.ethereum.privacy.markertransaction.FixedKeySigningPrivateMarkerTransactionFactory;
import org.enterchain.enter.ethereum.privacy.markertransaction.PrivateMarkerTransactionFactory;
import org.enterchain.enter.ethereum.privacy.markertransaction.RandomSigningPrivateMarkerTransactionFactory;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class PrivacyApiGroupJsonRpcMethods extends ApiGroupJsonRpcMethods {

  private final BlockchainQueries blockchainQueries;
  private final ProtocolSchedule protocolSchedule;
  private final TransactionPool transactionPool;
  private final PrivacyParameters privacyParameters;
  private final PrivateNonceProvider privateNonceProvider;
  private final PrivacyQueries privacyQueries;

  protected PrivacyApiGroupJsonRpcMethods(
      final BlockchainQueries blockchainQueries,
      final ProtocolSchedule protocolSchedule,
      final TransactionPool transactionPool,
      final PrivacyParameters privacyParameters) {
    this.blockchainQueries = blockchainQueries;
    this.protocolSchedule = protocolSchedule;
    this.transactionPool = transactionPool;
    this.privacyParameters = privacyParameters;

    this.privateNonceProvider =
        new ChainHeadPrivateNonceProvider(
            blockchainQueries.getBlockchain(),
            privacyParameters.getPrivateStateRootResolver(),
            privacyParameters.getPrivateWorldStateArchive());

    this.privacyQueries =
        new PrivacyQueries(blockchainQueries, privacyParameters.getPrivateWorldStateReader());
  }

  public BlockchainQueries getBlockchainQueries() {
    return blockchainQueries;
  }

  public ProtocolSchedule getProtocolSchedule() {
    return protocolSchedule;
  }

  public TransactionPool getTransactionPool() {
    return transactionPool;
  }

  public PrivacyParameters getPrivacyParameters() {
    return privacyParameters;
  }

  @Override
  protected Map<String, JsonRpcMethod> create() {
    final PrivateMarkerTransactionFactory markerTransactionFactory =
        createPrivateMarkerTransactionFactory(
            privacyParameters, blockchainQueries, transactionPool.getPendingTransactions());
    final EnclavePublicKeyProvider enclavePublicProvider =
        EnclavePublicKeyProvider.build(privacyParameters);
    final PrivacyController privacyController = createPrivacyController(markerTransactionFactory);
    return create(privacyController, enclavePublicProvider).entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry -> createPrivacyMethod(privacyParameters, entry.getValue())));
  }

  protected abstract Map<String, JsonRpcMethod> create(
      final PrivacyController privacyController,
      final EnclavePublicKeyProvider enclavePublicKeyProvider);

  private PrivateMarkerTransactionFactory createPrivateMarkerTransactionFactory(
      final PrivacyParameters privacyParameters,
      final BlockchainQueries blockchainQueries,
      final PendingTransactions pendingTransactions) {

    final Address privateContractAddress =
        Address.privacyPrecompiled(privacyParameters.getPrivacyAddress());

    if (privacyParameters.getSigningKeyPair().isPresent()) {
      return new FixedKeySigningPrivateMarkerTransactionFactory(
          privateContractAddress,
          new LatestNonceProvider(blockchainQueries, pendingTransactions),
          privacyParameters.getSigningKeyPair().get());
    }
    return new RandomSigningPrivateMarkerTransactionFactory(privateContractAddress);
  }

  private PrivacyController createPrivacyController(
      final PrivateMarkerTransactionFactory markerTransactionFactory) {
    final Optional<BigInteger> chainId = protocolSchedule.getChainId();
    final DefaultPrivacyController defaultPrivacyController =
        new DefaultPrivacyController(
            getBlockchainQueries().getBlockchain(),
            privacyParameters,
            chainId,
            markerTransactionFactory,
            createPrivateTransactionSimulator(),
            privateNonceProvider,
            privacyParameters.getPrivateWorldStateReader());
    return privacyParameters.isMultiTenancyEnabled()
        ? new MultiTenancyPrivacyController(
            defaultPrivacyController,
            chainId,
            privacyParameters.getEnclave(),
            privacyParameters.isOnchainPrivacyGroupsEnabled())
        : defaultPrivacyController;
  }

  PrivacyQueries getPrivacyQueries() {
    return privacyQueries;
  }

  private JsonRpcMethod createPrivacyMethod(
      final PrivacyParameters privacyParameters, final JsonRpcMethod rpcMethod) {
    final String methodName = rpcMethod.getName();
    if (methodName.equals(RpcMethod.ETH_SEND_RAW_PRIVATE_TRANSACTION.getMethodName())
        || methodName.equals(RpcMethod.GOQUORUM_STORE_RAW.getMethodName())) {
      return rpcMethod;
    } else if (privacyParameters.isEnabled() && privacyParameters.isMultiTenancyEnabled()) {
      return new MultiTenancyRpcMethodDecorator(rpcMethod);
    } else if (!privacyParameters.isEnabled()) {
      return new DisabledPrivacyRpcMethod(methodName);
    } else {
      return rpcMethod;
    }
  }

  private PrivateTransactionSimulator createPrivateTransactionSimulator() {
    return new PrivateTransactionSimulator(
        getBlockchainQueries().getBlockchain(),
        getBlockchainQueries().getWorldStateArchive(),
        getProtocolSchedule(),
        getPrivacyParameters());
  }
}
