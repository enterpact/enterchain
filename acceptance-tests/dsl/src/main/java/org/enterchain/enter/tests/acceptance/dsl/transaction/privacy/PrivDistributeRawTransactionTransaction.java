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
package org.enterchain.enter.tests.acceptance.dsl.transaction.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import org.enterchain.enter.tests.acceptance.dsl.transaction.NodeRequests;
import org.enterchain.enter.tests.acceptance.dsl.transaction.Transaction;

import java.io.IOException;

public class PrivDistributeRawTransactionTransaction implements Transaction<String> {

  final String transaction;

  public PrivDistributeRawTransactionTransaction(final String transaction) {
    this.transaction = transaction;
  }

  @Override
  public String execute(final NodeRequests node) {
    try {
      final PrivacyRequestFactory.PrivDistributeTransactionResponse result =
          node.privacy().privDistributeTransaction(transaction).send();
      assertThat(result).isNotNull();
      if (result.hasError()) {
        throw new RuntimeException(result.getError().getMessage());
      }
      return result.getTransactionKey();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
