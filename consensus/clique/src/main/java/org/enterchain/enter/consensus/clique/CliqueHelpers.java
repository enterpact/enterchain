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
package org.enterchain.enter.consensus.clique;

import org.enterchain.enter.consensus.clique.blockcreation.CliqueProposerSelector;
import org.enterchain.enter.consensus.common.ValidatorProvider;
import org.enterchain.enter.consensus.common.VoteTally;
import org.enterchain.enter.consensus.common.VoteTallyCache;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.BlockHeader;

public class CliqueHelpers {

  public static Address getProposerOfBlock(final BlockHeader header) {
    final CliqueExtraData extraData = CliqueExtraData.decode(header);
    return extraData.getProposerAddress();
  }

  public static Address getProposerForBlockAfter(
      final BlockHeader parent, final VoteTallyCache voteTallyCache) {
    final CliqueProposerSelector proposerSelector = new CliqueProposerSelector(voteTallyCache);
    return proposerSelector.selectProposerForNextBlock(parent);
  }

  public static boolean isSigner(
      final Address candidate, final ProtocolContext protocolContext, final BlockHeader parent) {
    final VoteTally validatorProvider =
        protocolContext
            .getConsensusState(CliqueContext.class)
            .getVoteTallyCache()
            .getVoteTallyAfterBlock(parent);
    return validatorProvider.getValidators().contains(candidate);
  }

  public static boolean addressIsAllowedToProduceNextBlock(
      final Address candidate, final ProtocolContext protocolContext, final BlockHeader parent) {
    final VoteTally validatorProvider =
        protocolContext
            .getConsensusState(CliqueContext.class)
            .getVoteTallyCache()
            .getVoteTallyAfterBlock(parent);

    if (!isSigner(candidate, protocolContext, parent)) {
      return false;
    }

    final int minimumUnsignedPastBlocks = minimumBlocksSincePreviousSigning(validatorProvider);

    final Blockchain blockchain = protocolContext.getBlockchain();
    int unsignedBlockCount = 0;
    BlockHeader localParent = parent;

    while (unsignedBlockCount < minimumUnsignedPastBlocks) {

      if (localParent.getNumber() == 0) {
        return true;
      }

      final Address parentSigner = CliqueHelpers.getProposerOfBlock(localParent);
      if (parentSigner.equals(candidate)) {
        return false;
      }
      unsignedBlockCount++;

      localParent =
          blockchain
              .getBlockHeader(localParent.getParentHash())
              .orElseThrow(() -> new IllegalStateException("The block was on a orphaned chain."));
    }

    return true;
  }

  private static int minimumBlocksSincePreviousSigning(final ValidatorProvider validatorProvider) {
    final int validatorCount = validatorProvider.getValidators().size();
    // The number of contiguous blocks in which a signer may only sign 1 (as taken from clique spec)
    final int signerLimit = (validatorCount / 2) + 1;
    return signerLimit - 1;
  }
}
