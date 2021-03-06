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
package org.enterchain.enter.tests.acceptance.dsl.transaction.privacy.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.enterchain.enter.tests.acceptance.dsl.transaction.NodeRequests;
import org.enterchain.enter.tests.acceptance.dsl.transaction.Transaction;

import java.io.IOException;
import java.util.List;

import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.EthLog.LogResult;

@SuppressWarnings("rawtypes")
public class PrivGetFilterChangesTransaction implements Transaction<List<LogResult>> {

  private final String privacyGroupId;
  private final String filterId;

  public PrivGetFilterChangesTransaction(final String privacyGroupId, final String filterId) {
    this.privacyGroupId = privacyGroupId;
    this.filterId = filterId;
  }

  @Override
  public List<LogResult> execute(final NodeRequests node) {
    try {
      final EthLog response = node.privacy().privGetFilterChanges(privacyGroupId, filterId).send();

      assertThat(response).as("check response is not null").isNotNull();
      assertThat(response.getResult()).as("check result in response isn't null").isNotNull();
      assertThat(response.getLogs()).isNotNull();

      return response.getLogs();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
