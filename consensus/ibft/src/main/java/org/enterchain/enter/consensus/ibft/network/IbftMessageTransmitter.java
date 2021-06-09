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
package org.enterchain.enter.consensus.ibft.network;

import org.enterchain.enter.consensus.common.bft.ConsensusRoundIdentifier;
import org.enterchain.enter.consensus.common.bft.network.ValidatorMulticaster;
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
import org.enterchain.enter.plugin.services.securitymodule.SecurityModuleException;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IbftMessageTransmitter {

  private static final Logger LOG = LogManager.getLogger();

  private final MessageFactory messageFactory;
  private final ValidatorMulticaster multicaster;

  public IbftMessageTransmitter(
      final MessageFactory messageFactory, final ValidatorMulticaster multicaster) {
    this.messageFactory = messageFactory;
    this.multicaster = multicaster;
  }

  public void multicastProposal(
      final ConsensusRoundIdentifier roundIdentifier,
      final Block block,
      final Optional<RoundChangeCertificate> roundChangeCertificate) {
    try {
      final Proposal data =
          messageFactory.createProposal(roundIdentifier, block, roundChangeCertificate);

      final ProposalMessageData message = ProposalMessageData.create(data);

      multicaster.send(message);
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to generate signature for Proposal (not sent): {} ", e.getMessage());
    }
  }

  public void multicastPrepare(final ConsensusRoundIdentifier roundIdentifier, final Hash digest) {
    try {
      final Prepare data = messageFactory.createPrepare(roundIdentifier, digest);

      final PrepareMessageData message = PrepareMessageData.create(data);

      multicaster.send(message);
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to generate signature for Prepare (not sent): {} ", e.getMessage());
    }
  }

  public void multicastCommit(
      final ConsensusRoundIdentifier roundIdentifier,
      final Hash digest,
      final SECPSignature commitSeal) {
    try {
      final Commit data = messageFactory.createCommit(roundIdentifier, digest, commitSeal);

      final CommitMessageData message = CommitMessageData.create(data);

      multicaster.send(message);
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to generate signature for Commit (not sent): {} ", e.getMessage());
    }
  }

  public void multicastRoundChange(
      final ConsensusRoundIdentifier roundIdentifier,
      final Optional<PreparedRoundArtifacts> preparedRoundArtifacts) {
    try {
      final RoundChange data =
          messageFactory.createRoundChange(roundIdentifier, preparedRoundArtifacts);

      final RoundChangeMessageData message = RoundChangeMessageData.create(data);

      multicaster.send(message);
    } catch (final SecurityModuleException e) {
      LOG.warn("Failed to generate signature for RoundChange (not sent): {} ", e.getMessage());
    }
  }
}
