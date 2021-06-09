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
package org.enterchain.enter.consensus.ibft.support;

import org.enterchain.enter.consensus.common.bft.BftBlockHashing;
import org.enterchain.enter.consensus.common.bft.BftExtraData;
import org.enterchain.enter.consensus.common.bft.ConsensusRoundIdentifier;
import org.enterchain.enter.consensus.common.bft.payload.SignedData;
import org.enterchain.enter.consensus.ibft.IbftExtraDataCodec;
import org.enterchain.enter.consensus.ibft.messagewrappers.Prepare;
import org.enterchain.enter.consensus.ibft.payload.CommitPayload;
import org.enterchain.enter.consensus.ibft.payload.MessageFactory;
import org.enterchain.enter.consensus.ibft.statemachine.PreparedRoundArtifacts;
import org.enterchain.enter.crypto.NodeKey;
import org.enterchain.enter.crypto.SECPSignature;
import org.enterchain.enter.ethereum.core.Block;

import java.util.Optional;
import java.util.stream.Collectors;

public class IntegrationTestHelpers {

  public static SignedData<CommitPayload> createSignedCommitPayload(
      final ConsensusRoundIdentifier roundId, final Block block, final NodeKey nodeKey) {

    final IbftExtraDataCodec ibftExtraDataEncoder = new IbftExtraDataCodec();
    final BftExtraData extraData = ibftExtraDataEncoder.decode(block.getHeader());

    final SECPSignature commitSeal =
        nodeKey.sign(
            new BftBlockHashing(ibftExtraDataEncoder)
                .calculateDataHashForCommittedSeal(block.getHeader(), extraData));

    final MessageFactory messageFactory = new MessageFactory(nodeKey);

    return messageFactory.createCommit(roundId, block.getHash(), commitSeal).getSignedPayload();
  }

  public static PreparedRoundArtifacts createValidPreparedRoundArtifacts(
      final TestContext context, final ConsensusRoundIdentifier preparedRound, final Block block) {
    final RoundSpecificPeers peers = context.roundSpecificPeers(preparedRound);

    return new PreparedRoundArtifacts(
        peers
            .getProposer()
            .getMessageFactory()
            .createProposal(preparedRound, block, Optional.empty()),
        peers.createSignedPreparePayloadOfNonProposing(preparedRound, block.getHash()).stream()
            .map(Prepare::new)
            .collect(Collectors.toList()));
  }
}
