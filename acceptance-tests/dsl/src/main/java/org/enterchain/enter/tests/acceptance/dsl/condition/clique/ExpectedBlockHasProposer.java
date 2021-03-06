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
package org.enterchain.enter.tests.acceptance.dsl.condition.clique;

import static org.assertj.core.api.Java6Assertions.assertThat;

import org.enterchain.enter.consensus.clique.CliqueBlockHeaderFunctions;
import org.enterchain.enter.consensus.clique.CliqueExtraData;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.tests.acceptance.dsl.BlockUtils;
import org.enterchain.enter.tests.acceptance.dsl.WaitUtils;
import org.enterchain.enter.tests.acceptance.dsl.condition.Condition;
import org.enterchain.enter.tests.acceptance.dsl.node.Node;
import org.enterchain.enter.tests.acceptance.dsl.transaction.eth.EthTransactions;

import org.web3j.protocol.core.methods.response.EthBlock.Block;

public class ExpectedBlockHasProposer implements Condition {
  private final EthTransactions eth;
  private final Address proposer;

  public ExpectedBlockHasProposer(final EthTransactions eth, final Address proposer) {
    this.eth = eth;
    this.proposer = proposer;
  }

  @Override
  public void verify(final Node node) {
    WaitUtils.waitFor(() -> assertThat(proposerAddress(node)).isEqualTo(proposer));
  }

  private Address proposerAddress(final Node node) {
    final Block block = node.execute(eth.block());
    final BlockHeader blockHeader =
        BlockUtils.createBlockHeader(block, new CliqueBlockHeaderFunctions());
    final CliqueExtraData cliqueExtraData = CliqueExtraData.decode(blockHeader);
    return cliqueExtraData.getProposerAddress();
  }
}
