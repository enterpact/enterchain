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
package org.enterchain.enter.consensus.qbft.jsonrpc;

import org.enterchain.enter.consensus.common.BlockInterface;
import org.enterchain.enter.consensus.common.EpochManager;
import org.enterchain.enter.consensus.common.VoteProposer;
import org.enterchain.enter.consensus.common.VoteTallyCache;
import org.enterchain.enter.consensus.common.VoteTallyUpdater;
import org.enterchain.enter.consensus.common.bft.BftBlockInterface;
import org.enterchain.enter.consensus.common.bft.BftContext;
import org.enterchain.enter.consensus.qbft.jsonrpc.methods.QbftDiscardValidatorVote;
import org.enterchain.enter.consensus.qbft.jsonrpc.methods.QbftGetPendingVotes;
import org.enterchain.enter.consensus.qbft.jsonrpc.methods.QbftGetSignerMetrics;
import org.enterchain.enter.consensus.qbft.jsonrpc.methods.QbftGetValidatorsByBlockHash;
import org.enterchain.enter.consensus.qbft.jsonrpc.methods.QbftGetValidatorsByBlockNumber;
import org.enterchain.enter.consensus.qbft.jsonrpc.methods.QbftProposeValidatorVote;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.api.jsonrpc.RpcApi;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.methods.ApiGroupJsonRpcMethods;
import org.enterchain.enter.ethereum.api.query.BlockchainQueries;
import org.enterchain.enter.ethereum.chain.MutableBlockchain;

import java.util.Map;

public class QbftJsonRpcMethods extends ApiGroupJsonRpcMethods {

  private final ProtocolContext context;

  public QbftJsonRpcMethods(final ProtocolContext context) {
    this.context = context;
  }

  @Override
  protected RpcApi getApiGroup() {
    return QbftRpcApis.QBFT;
  }

  @Override
  protected Map<String, JsonRpcMethod> create() {
    final MutableBlockchain mutableBlockchain = context.getBlockchain();
    final BlockchainQueries blockchainQueries =
        new BlockchainQueries(context.getBlockchain(), context.getWorldStateArchive());
    final BftContext bftContext = context.getConsensusState(BftContext.class);
    final VoteProposer voteProposer = bftContext.getVoteProposer();
    final BlockInterface blockInterface = bftContext.getBlockInterface();

    final VoteTallyCache voteTallyCache = createVoteTallyCache(context, mutableBlockchain);

    return mapOf(
        new QbftProposeValidatorVote(voteProposer),
        new QbftGetValidatorsByBlockNumber(blockchainQueries, blockInterface),
        new QbftDiscardValidatorVote(voteProposer),
        new QbftGetValidatorsByBlockHash(context.getBlockchain(), blockInterface),
        new QbftGetSignerMetrics(voteTallyCache, blockInterface, blockchainQueries),
        new QbftGetPendingVotes(voteProposer));
  }

  private VoteTallyCache createVoteTallyCache(
      final ProtocolContext context, final MutableBlockchain blockchain) {
    final BftContext bftContext = context.getConsensusState(BftContext.class);
    final EpochManager epochManager = bftContext.getEpochManager();
    final BftBlockInterface bftBlockInterface = bftContext.getBlockInterface();
    final VoteTallyUpdater voteTallyUpdater = new VoteTallyUpdater(epochManager, bftBlockInterface);
    return new VoteTallyCache(blockchain, voteTallyUpdater, epochManager, bftBlockInterface);
  }
}
