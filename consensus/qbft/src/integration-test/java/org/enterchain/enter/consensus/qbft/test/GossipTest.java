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
package org.enterchain.enter.consensus.qbft.test;

import static java.util.Collections.emptyList;

import org.enterchain.enter.consensus.common.bft.BftHelpers;
import org.enterchain.enter.consensus.common.bft.ConsensusRoundIdentifier;
import org.enterchain.enter.consensus.common.bft.events.NewChainHead;
import org.enterchain.enter.consensus.qbft.QbftExtraDataCodec;
import org.enterchain.enter.consensus.qbft.messagedata.ProposalMessageData;
import org.enterchain.enter.consensus.qbft.messagewrappers.Commit;
import org.enterchain.enter.consensus.qbft.messagewrappers.Prepare;
import org.enterchain.enter.consensus.qbft.messagewrappers.Proposal;
import org.enterchain.enter.consensus.qbft.messagewrappers.RoundChange;
import org.enterchain.enter.consensus.qbft.payload.MessageFactory;
import org.enterchain.enter.consensus.qbft.support.RoundSpecificPeers;
import org.enterchain.enter.consensus.qbft.support.TestContext;
import org.enterchain.enter.consensus.qbft.support.TestContextBuilder;
import org.enterchain.enter.consensus.qbft.support.ValidatorPeer;
import org.enterchain.enter.crypto.NodeKeyUtils;
import org.enterchain.enter.ethereum.core.Block;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

public class GossipTest {

  private final long blockTimeStamp = 100;
  private final Clock fixedClock =
      Clock.fixed(Instant.ofEpochSecond(blockTimeStamp), ZoneId.systemDefault());

  private final int NETWORK_SIZE = 5;

  private final TestContext context =
      new TestContextBuilder()
          .validatorCount(NETWORK_SIZE)
          .indexOfFirstLocallyProposedBlock(0)
          .clock(fixedClock)
          .useGossip(true)
          .buildAndStart();

  private final ConsensusRoundIdentifier roundId = new ConsensusRoundIdentifier(1, 0);
  private final RoundSpecificPeers peers = context.roundSpecificPeers(roundId);
  private Block block;
  private ValidatorPeer sender;
  private MessageFactory msgFactory;

  @Before
  public void setup() {
    block = context.createBlockForProposalFromChainHead(30, peers.getProposer().getNodeAddress());
    sender = peers.getProposer();
    msgFactory = sender.getMessageFactory();
  }

  @Test
  public void gossipMessagesToPeers() {
    final Prepare localPrepare =
        context.getLocalNodeMessageFactory().createPrepare(roundId, block.getHash());
    peers.verifyNoMessagesReceivedNonProposing();

    final Proposal proposal = sender.injectProposal(roundId, block);
    // sender node will have a prepare message as an effect of the proposal being sent
    peers.verifyMessagesReceivedNonPropsing(proposal, localPrepare);
    peers.verifyMessagesReceivedProposer(localPrepare);

    final Prepare prepare = sender.injectPrepare(roundId, block.getHash());
    peers.verifyMessagesReceivedNonPropsing(prepare);
    peers.verifyNoMessagesReceivedProposer();

    final Commit commit = sender.injectCommit(roundId, block);
    peers.verifyMessagesReceivedNonPropsing(commit);
    peers.verifyNoMessagesReceivedProposer();

    final RoundChange roundChange = msgFactory.createRoundChange(roundId, Optional.empty());

    final Proposal nextRoundProposal =
        sender.injectProposalForFutureRound(
            roundId,
            Collections.singletonList(roundChange.getSignedPayload()),
            roundChange.getPrepares(),
            block);
    peers.verifyMessagesReceivedNonPropsing(nextRoundProposal);
    peers.verifyNoMessagesReceivedProposer();

    sender.injectRoundChange(roundId, Optional.empty());
    peers.verifyMessagesReceivedNonPropsing(roundChange);
    peers.verifyNoMessagesReceivedProposer();
  }

  @Test
  public void onlyGossipOnce() {
    final Prepare prepare = sender.injectPrepare(roundId, block.getHash());
    peers.verifyMessagesReceivedNonPropsing(prepare);

    sender.injectPrepare(roundId, block.getHash());
    peers.verifyNoMessagesReceivedNonProposing();

    sender.injectPrepare(roundId, block.getHash());
    peers.verifyNoMessagesReceivedNonProposing();
  }

  @Test
  public void messageWithUnknownValidatorIsNotGossiped() {
    final MessageFactory unknownMsgFactory = new MessageFactory(NodeKeyUtils.generate());
    final Proposal unknownProposal =
        unknownMsgFactory.createProposal(roundId, block, emptyList(), emptyList());

    sender.injectMessage(ProposalMessageData.create(unknownProposal));
    peers.verifyNoMessagesReceived();
  }

  @Test
  public void messageIsNotGossipedToSenderOrCreator() {
    final ValidatorPeer msgCreator = peers.getFirstNonProposer();
    final MessageFactory peerMsgFactory = msgCreator.getMessageFactory();
    final Proposal proposalFromPeer =
        peerMsgFactory.createProposal(roundId, block, emptyList(), emptyList());

    sender.injectMessage(ProposalMessageData.create(proposalFromPeer));

    peers.verifyMessagesReceivedNonProposingExcluding(msgCreator, proposalFromPeer);
    peers.verifyNoMessagesReceivedProposer();
  }

  @Test
  public void futureMessageIsNotGossipedImmediately() {
    ConsensusRoundIdentifier futureRoundId = new ConsensusRoundIdentifier(2, 0);
    msgFactory.createProposal(futureRoundId, block, emptyList(), emptyList());

    sender.injectProposal(futureRoundId, block);
    peers.verifyNoMessagesReceived();
  }

  @Test
  public void previousHeightMessageIsNotGossiped() {
    final ConsensusRoundIdentifier futureRoundId = new ConsensusRoundIdentifier(0, 0);
    sender.injectProposal(futureRoundId, block);
    peers.verifyNoMessagesReceived();
  }

  @Test
  public void futureMessageGetGossipedLater() {
    final Block signedCurrentHeightBlock =
        BftHelpers.createSealedBlock(
            new QbftExtraDataCodec(), block, 0, peers.sign(block.getHash()));

    final ConsensusRoundIdentifier futureRoundId = new ConsensusRoundIdentifier(2, 0);
    final Prepare futurePrepare = sender.injectPrepare(futureRoundId, block.getHash());
    peers.verifyNoMessagesReceivedNonProposing();

    // add block to chain so we can move to next block height
    context.getBlockchain().appendBlock(signedCurrentHeightBlock, emptyList());
    context
        .getController()
        .handleNewBlockEvent(new NewChainHead(signedCurrentHeightBlock.getHeader()));

    peers.verifyMessagesReceivedNonPropsing(futurePrepare);
  }
}
