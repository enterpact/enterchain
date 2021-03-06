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
package org.enterchain.enter.consensus.qbft.support;

import org.enterchain.enter.consensus.common.bft.BftBlockHashing;
import org.enterchain.enter.consensus.common.bft.BftBlockHeaderFunctions;
import org.enterchain.enter.consensus.common.bft.BftBlockInterface;
import org.enterchain.enter.consensus.common.bft.ConsensusRoundIdentifier;
import org.enterchain.enter.consensus.common.bft.payload.SignedData;
import org.enterchain.enter.consensus.qbft.QbftExtraDataCodec;
import org.enterchain.enter.consensus.qbft.payload.CommitPayload;
import org.enterchain.enter.consensus.qbft.payload.MessageFactory;
import org.enterchain.enter.consensus.qbft.statemachine.PreparedCertificate;
import org.enterchain.enter.crypto.NodeKey;
import org.enterchain.enter.crypto.SECPSignature;
import org.enterchain.enter.ethereum.core.Block;

public class IntegrationTestHelpers {

  public static SignedData<CommitPayload> createSignedCommitPayload(
      final ConsensusRoundIdentifier roundId, final Block block, final NodeKey nodeKey) {

    final QbftExtraDataCodec ibftExtraDataEncoder = new QbftExtraDataCodec();

    final Block commitBlock = createCommitBlockFromProposalBlock(block, roundId.getRoundNumber());
    final SECPSignature commitSeal =
        nodeKey.sign(
            new BftBlockHashing(ibftExtraDataEncoder)
                .calculateDataHashForCommittedSeal(commitBlock.getHeader()));

    final MessageFactory messageFactory = new MessageFactory(nodeKey);

    return messageFactory.createCommit(roundId, block.getHash(), commitSeal).getSignedPayload();
  }

  public static PreparedCertificate createValidPreparedCertificate(
      final TestContext context, final ConsensusRoundIdentifier preparedRound, final Block block) {
    final RoundSpecificPeers peers = context.roundSpecificPeers(preparedRound);

    return new PreparedCertificate(
        block,
        peers.createSignedPreparePayloadOfAllPeers(preparedRound, block.getHash()),
        preparedRound.getRoundNumber());
  }

  public static Block createCommitBlockFromProposalBlock(
      final Block proposalBlock, final int round) {
    final QbftExtraDataCodec bftExtraDataCodec = new QbftExtraDataCodec();
    final BftBlockInterface bftBlockInterface = new BftBlockInterface(bftExtraDataCodec);
    return bftBlockInterface.replaceRoundInBlock(
        proposalBlock, round, BftBlockHeaderFunctions.forCommittedSeal(bftExtraDataCodec));
  }
}
