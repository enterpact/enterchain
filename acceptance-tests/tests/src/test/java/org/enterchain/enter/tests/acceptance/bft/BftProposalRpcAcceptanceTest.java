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
package org.enterchain.enter.tests.acceptance.bft;

import org.enterchain.enter.tests.acceptance.dsl.node.BesuNode;

import org.junit.Test;

public class BftProposalRpcAcceptanceTest extends ParameterizedBftTestBase {

  public BftProposalRpcAcceptanceTest(
      final String testName, final BftAcceptanceTestParameterization nodeFactory) {
    super(testName, nodeFactory);
  }

  @Test
  public void shouldReturnProposals() throws Exception {
    final String[] validators = {"validator1", "validator2", "validator3"};
    final BesuNode validator1 =
        nodeFactory.createNodeWithValidators(besu, "validator1", validators);
    final BesuNode validator2 =
        nodeFactory.createNodeWithValidators(besu, "validator2", validators);
    final BesuNode validator3 =
        nodeFactory.createNodeWithValidators(besu, "validator3", validators);
    cluster.start(validator1, validator2, validator3);

    cluster.verify(bft.noProposals());
    validator1.execute(bftTransactions.createAddProposal(validator3));
    validator1.execute(bftTransactions.createRemoveProposal(validator2));
    validator2.execute(bftTransactions.createRemoveProposal(validator3));

    validator1.verify(
        bft.pendingVotesEqual().addProposal(validator3).removeProposal(validator2).build());
    validator2.verify(bft.pendingVotesEqual().removeProposal(validator3).build());
    validator3.verify(bft.noProposals());
  }
}
