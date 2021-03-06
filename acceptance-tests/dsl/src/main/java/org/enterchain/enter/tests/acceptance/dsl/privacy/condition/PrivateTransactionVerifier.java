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
package org.enterchain.enter.tests.acceptance.dsl.privacy.condition;

import org.enterchain.enter.tests.acceptance.dsl.privacy.PrivacyNode;
import org.enterchain.enter.tests.acceptance.dsl.privacy.transaction.PrivacyTransactions;
import org.enterchain.enter.tests.acceptance.dsl.transaction.privacy.PrivacyRequestFactory.OnChainPrivacyGroup;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.web3j.protocol.besu.response.privacy.PrivacyGroup;
import org.web3j.protocol.besu.response.privacy.PrivateTransactionReceipt;
import org.web3j.utils.Base64String;

public class PrivateTransactionVerifier {

  private final PrivacyTransactions transactions;

  public PrivateTransactionVerifier(final PrivacyTransactions transactions) {
    this.transactions = transactions;
  }

  public ExpectValidPrivateTransactionReceipt validPrivateTransactionReceipt(
      final String transactionHash, final PrivateTransactionReceipt receipt) {
    return new ExpectValidPrivateTransactionReceipt(transactions, transactionHash, receipt);
  }

  public ExpectExistingPrivateTransactionReceipt existingPrivateTransactionReceipt(
      final String transactionHash) {
    return new ExpectExistingPrivateTransactionReceipt(transactions, transactionHash);
  }

  public ExpectNoPrivateTransactionReceipt noPrivateTransactionReceipt(
      final String transactionHash) {
    return new ExpectNoPrivateTransactionReceipt(transactions, transactionHash);
  }

  public ExpectValidPrivacyGroupCreated validPrivacyGroupCreated(final PrivacyGroup expected) {
    return new ExpectValidPrivacyGroupCreated(transactions, expected);
  }

  public ExpectValidOnChainPrivacyGroupCreated onChainPrivacyGroupExists(
      final String privacyGroupId, final PrivacyNode... members) {

    final List<Base64String> membersEnclaveKeys =
        Arrays.stream(members)
            .map(PrivacyNode::getEnclaveKey)
            .map(Base64String::wrap)
            .collect(Collectors.toList());
    return onChainPrivacyGroupExists(privacyGroupId, membersEnclaveKeys);
  }

  public ExpectValidOnChainPrivacyGroupCreated onChainPrivacyGroupExists(
      final String privacyGroupId, final List<Base64String> membersEnclaveKeys) {

    final OnChainPrivacyGroup expectedGroup =
        new OnChainPrivacyGroup(privacyGroupId, membersEnclaveKeys);

    return new ExpectValidOnChainPrivacyGroupCreated(transactions, expectedGroup);
  }

  public ExpectInternalErrorPrivateTransactionReceipt internalErrorPrivateTransactionReceipt(
      final String transactionHash) {
    return new ExpectInternalErrorPrivateTransactionReceipt(transactions, transactionHash);
  }
}
