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
package org.enterchain.enter.consensus.ibft.payload;

import org.enterchain.enter.consensus.common.bft.ConsensusRoundIdentifier;
import org.enterchain.enter.consensus.common.bft.payload.Payload;
import org.enterchain.enter.consensus.common.bft.payload.SignedData;
import org.enterchain.enter.consensus.ibft.messagewrappers.Commit;
import org.enterchain.enter.consensus.ibft.messagewrappers.Prepare;
import org.enterchain.enter.consensus.ibft.messagewrappers.Proposal;
import org.enterchain.enter.consensus.ibft.messagewrappers.RoundChange;
import org.enterchain.enter.consensus.ibft.statemachine.PreparedRoundArtifacts;
import org.enterchain.enter.crypto.NodeKey;
import org.enterchain.enter.crypto.SECPSignature;
import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.Hash;

import java.util.Optional;

public class MessageFactory {

  private final NodeKey nodeKey;

  public MessageFactory(final NodeKey nodeKey) {
    this.nodeKey = nodeKey;
  }

  public Proposal createProposal(
      final ConsensusRoundIdentifier roundIdentifier,
      final Block block,
      final Optional<RoundChangeCertificate> roundChangeCertificate) {

    final ProposalPayload payload = new ProposalPayload(roundIdentifier, block.getHash());

    return new Proposal(createSignedMessage(payload), block, roundChangeCertificate);
  }

  public Prepare createPrepare(final ConsensusRoundIdentifier roundIdentifier, final Hash digest) {

    final PreparePayload payload = new PreparePayload(roundIdentifier, digest);

    return new Prepare(createSignedMessage(payload));
  }

  public Commit createCommit(
      final ConsensusRoundIdentifier roundIdentifier,
      final Hash digest,
      final SECPSignature commitSeal) {

    final CommitPayload payload = new CommitPayload(roundIdentifier, digest, commitSeal);

    return new Commit(createSignedMessage(payload));
  }

  public RoundChange createRoundChange(
      final ConsensusRoundIdentifier roundIdentifier,
      final Optional<PreparedRoundArtifacts> preparedRoundArtifacts) {

    final RoundChangePayload payload =
        new RoundChangePayload(
            roundIdentifier,
            preparedRoundArtifacts.map(PreparedRoundArtifacts::getPreparedCertificate));
    return new RoundChange(
        createSignedMessage(payload), preparedRoundArtifacts.map(PreparedRoundArtifacts::getBlock));
  }

  private <M extends Payload> SignedData<M> createSignedMessage(final M payload) {
    final SECPSignature signature = nodeKey.sign(payload.hashForSignature());
    return SignedData.create(payload, signature);
  }
}
