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
package org.enterchain.enter.consensus.clique.jsonrpc;

import org.enterchain.enter.consensus.clique.CliqueBlockInterface;
import org.enterchain.enter.consensus.clique.CliqueContext;
import org.enterchain.enter.consensus.clique.jsonrpc.methods.CliqueGetSignerMetrics;
import org.enterchain.enter.consensus.clique.jsonrpc.methods.CliqueGetSigners;
import org.enterchain.enter.consensus.clique.jsonrpc.methods.CliqueGetSignersAtHash;
import org.enterchain.enter.consensus.clique.jsonrpc.methods.CliqueProposals;
import org.enterchain.enter.consensus.clique.jsonrpc.methods.Discard;
import org.enterchain.enter.consensus.clique.jsonrpc.methods.Propose;
import org.enterchain.enter.consensus.common.EpochManager;
import org.enterchain.enter.consensus.common.VoteProposer;
import org.enterchain.enter.consensus.common.VoteTallyCache;
import org.enterchain.enter.consensus.common.VoteTallyUpdater;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.api.jsonrpc.RpcApi;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.methods.ApiGroupJsonRpcMethods;
import org.enterchain.enter.ethereum.api.query.BlockchainQueries;
import org.enterchain.enter.ethereum.chain.MutableBlockchain;
import org.enterchain.enter.ethereum.worldstate.WorldStateArchive;

import java.util.Map;

public class CliqueJsonRpcMethods extends ApiGroupJsonRpcMethods {
  private final ProtocolContext context;

  public CliqueJsonRpcMethods(final ProtocolContext context) {
    this.context = context;
  }

  @Override
  protected RpcApi getApiGroup() {
    return CliqueRpcApis.CLIQUE;
  }

  @Override
  protected Map<String, JsonRpcMethod> create() {
    final MutableBlockchain blockchain = context.getBlockchain();
    final WorldStateArchive worldStateArchive = context.getWorldStateArchive();
    final BlockchainQueries blockchainQueries =
        new BlockchainQueries(blockchain, worldStateArchive);
    final VoteProposer voteProposer =
        context.getConsensusState(CliqueContext.class).getVoteProposer();

    // Must create our own voteTallyCache as using this would pollute the main voteTallyCache
    final VoteTallyCache voteTallyCache = createVoteTallyCache(context, blockchain);

    return mapOf(
        new CliqueGetSigners(blockchainQueries, voteTallyCache),
        new CliqueGetSignersAtHash(blockchainQueries, voteTallyCache),
        new Propose(voteProposer),
        new Discard(voteProposer),
        new CliqueProposals(voteProposer),
        new CliqueGetSignerMetrics(voteTallyCache, new CliqueBlockInterface(), blockchainQueries));
  }

  private VoteTallyCache createVoteTallyCache(
      final ProtocolContext context, final MutableBlockchain blockchain) {
    final EpochManager epochManager =
        context.getConsensusState(CliqueContext.class).getEpochManager();
    final CliqueBlockInterface cliqueBlockInterface = new CliqueBlockInterface();
    final VoteTallyUpdater voteTallyUpdater =
        new VoteTallyUpdater(epochManager, cliqueBlockInterface);
    return new VoteTallyCache(blockchain, voteTallyUpdater, epochManager, cliqueBlockInterface);
  }
}
