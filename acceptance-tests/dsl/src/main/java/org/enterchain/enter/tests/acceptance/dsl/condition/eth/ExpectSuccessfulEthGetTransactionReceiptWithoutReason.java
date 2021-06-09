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
package org.enterchain.enter.tests.acceptance.dsl.condition.eth;

import static org.assertj.core.api.Assertions.assertThat;

import org.enterchain.enter.tests.acceptance.dsl.WaitUtils;
import org.enterchain.enter.tests.acceptance.dsl.condition.Condition;
import org.enterchain.enter.tests.acceptance.dsl.node.Node;
import org.enterchain.enter.tests.acceptance.dsl.transaction.eth.EthGetTransactionReceiptWithRevertReason;
import org.enterchain.enter.tests.acceptance.dsl.transaction.net.CustomRequestFactory.TransactionReceiptWithRevertReason;

public class ExpectSuccessfulEthGetTransactionReceiptWithoutReason implements Condition {

  private final EthGetTransactionReceiptWithRevertReason transaction;

  public ExpectSuccessfulEthGetTransactionReceiptWithoutReason(
      final EthGetTransactionReceiptWithRevertReason transaction) {
    this.transaction = transaction;
  }

  @Override
  public void verify(final Node node) {
    WaitUtils.waitFor(() -> assertThat(revertReasonIsEmpty(node)).isTrue());
  }

  private boolean revertReasonIsEmpty(final Node node) {
    return node.execute(transaction)
        .map(TransactionReceiptWithRevertReason::getRevertReason)
        .filter(str -> str.equals("0x"))
        .isPresent();
  }
}