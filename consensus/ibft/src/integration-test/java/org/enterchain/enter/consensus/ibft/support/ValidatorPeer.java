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

import org.enterchain.enter.consensus.common.bft.ConsensusRoundIdentifier;
import org.enterchain.enter.consensus.common.bft.EventMultiplexer;
import org.enterchain.enter.consensus.common.bft.inttest.DefaultValidatorPeer;
import org.enterchain.enter.consensus.common.bft.inttest.NodeParams;
import org.enterchain.enter.consensus.ibft.messagedata.CommitMessageData;
import org.enterchain.enter.consensus.ibft.messagedata.PrepareMessageData;
import org.enterchain.enter.consensus.ibft.messagedata.ProposalMessageData;
import org.enterchain.enter.consensus.ibft.messagedata.RoundChangeMessageData;
import org.enterchain.enter.consensus.ibft.messagewrappers.Commit;
import org.enterchain.enter.consensus.ibft.messagewrappers.Prepare;
import org.enterchain.enter.consensus.ibft.messagewrappers.Proposal;
import org.enterchain.enter.consensus.ibft.messagewrappers.RoundChange;
import org.enterchain.enter.consensus.ibft.payload.MessageFactory;
import org.enterchain.enter.consensus.ibft.payload.RoundChangeCertificate;
import org.enterchain.enter.consensus.ibft.statemachine.PreparedRoundArtifacts;
import org.enterchain.enter.crypto.SECPSignature;
import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.Hash;

import java.util.Optional;

// Each "inject" function returns the SignedPayload representation of the transmitted message.
public class ValidatorPeer extends DefaultValidatorPeer {

  private final MessageFactory messageFactory;

  public ValidatorPeer(
      final NodeParams nodeParams,
      final MessageFactory messageFactory,
      final EventMultiplexer localEventMultiplexer) {
    super(nodeParams, localEventMultiplexer);
    this.messageFactory = messageFactory;
  }

  public Proposal injectProposal(final ConsensusRoundIdentifier rId, final Block block) {
    final Proposal payload = messageFactory.createProposal(rId, block, Optional.empty());

    injectMessage(ProposalMessageData.create(payload));
    return payload;
  }

  public Prepare injectPrepare(final ConsensusRoundIdentifier rId, final Hash digest) {
    final Prepare payload = messageFactory.createPrepare(rId, digest);
    injectMessage(PrepareMessageData.create(payload));
    return payload;
  }

  public Commit injectCommit(final ConsensusRoundIdentifier rId, final Hash digest) {
    final SECPSignature commitSeal = nodeKey.sign(digest);

    return injectCommit(rId, digest, commitSeal);
  }

  public Commit injectCommit(
      final ConsensusRoundIdentifier rId, final Hash digest, final SECPSignature commitSeal) {
    final Commit payload = messageFactory.createCommit(rId, digest, commitSeal);
    injectMessage(CommitMessageData.create(payload));
    return payload;
  }

  public Proposal injectProposalForFutureRound(
      final ConsensusRoundIdentifier rId,
      final RoundChangeCertificate roundChangeCertificate,
      final Block blockToPropose) {

    final Proposal payload =
        messageFactory.createProposal(rId, blockToPropose, Optional.of(roundChangeCertificate));
    injectMessage(ProposalMessageData.create(payload));
    return payload;
  }

  public RoundChange injectRoundChange(
      final ConsensusRoundIdentifier rId,
      final Optional<PreparedRoundArtifacts> preparedRoundArtifacts) {
    final RoundChange payload = messageFactory.createRoundChange(rId, preparedRoundArtifacts);
    injectMessage(RoundChangeMessageData.create(payload));
    return payload;
  }

  public MessageFactory getMessageFactory() {
    return messageFactory;
  }
}
