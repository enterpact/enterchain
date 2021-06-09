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
package org.enterchain.enter.ethereum.api.jsonrpc.websocket.methods;

import org.enterchain.enter.ethereum.api.jsonrpc.LatestNonceProvider;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.EnclavePublicKeyProvider;
import org.enterchain.enter.ethereum.api.jsonrpc.websocket.subscription.SubscriptionManager;
import org.enterchain.enter.ethereum.api.jsonrpc.websocket.subscription.request.SubscriptionRequestMapper;
import org.enterchain.enter.ethereum.api.query.BlockchainQueries;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.PrivacyParameters;
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
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public class PrivateWebSocketMethodsFactory {

  private final PrivacyParameters privacyParameters;
  private final SubscriptionManager subscriptionManager;
  private final ProtocolSchedule protocolSchedule;
  private final BlockchainQueries blockchainQueries;
  private final TransactionPool transactionPool;

  public PrivateWebSocketMethodsFactory(
      final PrivacyParameters privacyParameters,
      final SubscriptionManager subscriptionManager,
      final ProtocolSchedule protocolSchedule,
      final BlockchainQueries blockchainQueries,
      final TransactionPool transactionPool) {
    this.privacyParameters = privacyParameters;
    this.subscriptionManager = subscriptionManager;
    this.protocolSchedule = protocolSchedule;
    this.blockchainQueries = blockchainQueries;
    this.transactionPool = transactionPool;
  }

  public Collection<JsonRpcMethod> methods() {
    final SubscriptionRequestMapper subscriptionRequestMapper = new SubscriptionRequestMapper();
    final EnclavePublicKeyProvider enclavePublicKeyProvider =
        EnclavePublicKeyProvider.build(privacyParameters);
    final PrivacyController privacyController = createPrivacyController();

    return Set.of(
        new PrivSubscribe(
            subscriptionManager,
            subscriptionRequestMapper,
            privacyController,
            enclavePublicKeyProvider),
        new PrivUnsubscribe(
            subscriptionManager,
            subscriptionRequestMapper,
            privacyController,
            enclavePublicKeyProvider));
  }

  private PrivateMarkerTransactionFactory createPrivateMarkerTransactionFactory() {

    final Address privateContractAddress =
        Address.privacyPrecompiled(privacyParameters.getPrivacyAddress());

    if (privacyParameters.getSigningKeyPair().isPresent()) {
      return new FixedKeySigningPrivateMarkerTransactionFactory(
          privateContractAddress,
          new LatestNonceProvider(blockchainQueries, transactionPool.getPendingTransactions()),
          privacyParameters.getSigningKeyPair().get());
    }
    return new RandomSigningPrivateMarkerTransactionFactory(privateContractAddress);
  }

  private PrivacyController createPrivacyController() {
    final Optional<BigInteger> chainId = protocolSchedule.getChainId();
    final DefaultPrivacyController defaultPrivacyController =
        new DefaultPrivacyController(
            blockchainQueries.getBlockchain(),
            privacyParameters,
            chainId,
            createPrivateMarkerTransactionFactory(),
            createPrivateTransactionSimulator(),
            createPrivateNonceProvider(),
            privacyParameters.getPrivateWorldStateReader());
    return privacyParameters.isMultiTenancyEnabled()
        ? new MultiTenancyPrivacyController(
            defaultPrivacyController,
            chainId,
            privacyParameters.getEnclave(),
            privacyParameters.isOnchainPrivacyGroupsEnabled())
        : defaultPrivacyController;
  }

  private PrivateTransactionSimulator createPrivateTransactionSimulator() {
    return new PrivateTransactionSimulator(
        blockchainQueries.getBlockchain(),
        blockchainQueries.getWorldStateArchive(),
        protocolSchedule,
        privacyParameters);
  }

  private PrivateNonceProvider createPrivateNonceProvider() {
    return new ChainHeadPrivateNonceProvider(
        blockchainQueries.getBlockchain(),
        privacyParameters.getPrivateStateRootResolver(),
        privacyParameters.getPrivateWorldStateArchive());
  }
}
