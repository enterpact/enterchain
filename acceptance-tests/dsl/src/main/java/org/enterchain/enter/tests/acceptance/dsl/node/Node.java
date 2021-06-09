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
package org.enterchain.enter.tests.acceptance.dsl.node;

import org.enterchain.enter.crypto.SignatureAlgorithm;
import org.enterchain.enter.tests.acceptance.dsl.condition.Condition;
import org.enterchain.enter.tests.acceptance.dsl.transaction.Transaction;
import org.enterchain.enter.tests.acceptance.dsl.transaction.TransactionWithSignatureAlgorithm;

public interface Node {

  <T> T execute(Transaction<T> transaction);

  <T> T execute(
      TransactionWithSignatureAlgorithm<T> transaction, SignatureAlgorithm signatureAlgorithm);

  void verify(final Condition expected);
}