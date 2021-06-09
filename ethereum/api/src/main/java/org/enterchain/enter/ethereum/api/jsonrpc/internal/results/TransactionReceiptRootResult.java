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
package org.enterchain.enter.ethereum.api.jsonrpc.internal.results;

import org.enterchain.enter.ethereum.api.query.TransactionReceiptWithMetadata;

import com.fasterxml.jackson.annotation.JsonGetter;

public class TransactionReceiptRootResult extends TransactionReceiptResult {

  private final String root;

  public TransactionReceiptRootResult(final TransactionReceiptWithMetadata receiptWithMetadata) {
    super(receiptWithMetadata);
    root = receipt.getStateRoot().toString();
  }

  @JsonGetter(value = "root")
  public String getRoot() {
    return root;
  }
}
