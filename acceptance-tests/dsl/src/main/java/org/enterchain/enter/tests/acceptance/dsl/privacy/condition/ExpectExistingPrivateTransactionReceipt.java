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

import static org.assertj.core.api.Assertions.assertThat;

import org.enterchain.enter.tests.acceptance.dsl.privacy.PrivacyNode;
import org.enterchain.enter.tests.acceptance.dsl.privacy.transaction.PrivacyTransactions;

import org.web3j.protocol.besu.response.privacy.PrivateTransactionReceipt;

public class ExpectExistingPrivateTransactionReceipt implements PrivateCondition {
  private final PrivacyTransactions transactions;
  private final String transactionHash;

  public ExpectExistingPrivateTransactionReceipt(
      final PrivacyTransactions transactions, final String transactionHash) {

    this.transactions = transactions;
    this.transactionHash = transactionHash;
  }

  @Override
  public void verify(final PrivacyNode node) {
    final PrivateTransactionReceipt privateTransactionReceipt =
        node.execute(transactions.getPrivateTransactionReceipt(transactionHash));
    assertThat(privateTransactionReceipt).isNotNull();
  }
}
