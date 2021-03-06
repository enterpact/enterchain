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
package org.enterchain.enter.consensus.ibft;

import org.enterchain.enter.consensus.common.BlockInterface;
import org.enterchain.enter.consensus.common.EpochManager;
import org.enterchain.enter.consensus.common.PoaContext;
import org.enterchain.enter.consensus.common.VoteProposer;
import org.enterchain.enter.consensus.common.VoteTallyCache;

/** Holds the BFT specific mutable state. */
public class IbftLegacyContext implements PoaContext {

  private final VoteTallyCache voteTallyCache;
  private final VoteProposer voteProposer;
  private final EpochManager epochManager;
  private final BlockInterface blockInterface;

  public IbftLegacyContext(
      final VoteTallyCache voteTallyCache,
      final VoteProposer voteProposer,
      final EpochManager epochManager,
      final BlockInterface blockInterface) {
    this.voteTallyCache = voteTallyCache;
    this.voteProposer = voteProposer;
    this.epochManager = epochManager;
    this.blockInterface = blockInterface;
  }

  public VoteTallyCache getVoteTallyCache() {
    return voteTallyCache;
  }

  public VoteProposer getVoteProposer() {
    return voteProposer;
  }

  public EpochManager getEpochManager() {
    return epochManager;
  }

  @Override
  public BlockInterface getBlockInterface() {
    return blockInterface;
  }
}
