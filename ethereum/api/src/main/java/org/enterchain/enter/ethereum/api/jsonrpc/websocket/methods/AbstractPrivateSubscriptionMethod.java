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

import org.enterchain.enter.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.EnclavePublicKeyProvider;
import org.enterchain.enter.ethereum.api.jsonrpc.websocket.subscription.SubscriptionManager;
import org.enterchain.enter.ethereum.api.jsonrpc.websocket.subscription.request.SubscriptionRequestMapper;
import org.enterchain.enter.ethereum.privacy.PrivacyController;

abstract class AbstractPrivateSubscriptionMethod extends AbstractSubscriptionMethod {

  private final PrivacyController privacyController;
  protected final EnclavePublicKeyProvider enclavePublicKeyProvider;

  AbstractPrivateSubscriptionMethod(
      final SubscriptionManager subscriptionManager,
      final SubscriptionRequestMapper mapper,
      final PrivacyController privacyController,
      final EnclavePublicKeyProvider enclavePublicKeyProvider) {
    super(subscriptionManager, mapper);
    this.privacyController = privacyController;
    this.enclavePublicKeyProvider = enclavePublicKeyProvider;
  }

  void checkIfPrivacyGroupMatchesAuthenticatedEnclaveKey(
      final JsonRpcRequestContext request, final String privacyGroupId) {
    final String enclavePublicKey = enclavePublicKeyProvider.getEnclaveKey(request.getUser());
    privacyController.verifyPrivacyGroupContainsEnclavePublicKey(privacyGroupId, enclavePublicKey);
  }
}
