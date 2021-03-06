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
package org.enterchain.enter.consensus.ibft.validation;

import org.enterchain.enter.consensus.common.bft.BftBlockInterface;
import org.enterchain.enter.consensus.common.bft.BftExtraData;
import org.enterchain.enter.consensus.common.bft.ConsensusRoundIdentifier;
import org.enterchain.enter.consensus.common.bft.payload.SignedData;
import org.enterchain.enter.consensus.ibft.payload.ProposalPayload;
import org.enterchain.enter.ethereum.core.Block;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProposalBlockConsistencyValidator {

  private static final Logger LOG = LogManager.getLogger();

  public boolean validateProposalMatchesBlock(
      final SignedData<ProposalPayload> signedPayload,
      final Block proposedBlock,
      final BftBlockInterface bftBlockInterface) {

    if (!signedPayload.getPayload().getDigest().equals(proposedBlock.getHash())) {
      LOG.info("Invalid Proposal, embedded digest does not match block's hash.");
      return false;
    }

    if (proposedBlock.getHeader().getNumber()
        != signedPayload.getPayload().getRoundIdentifier().getSequenceNumber()) {
      LOG.info("Invalid proposal/block - message sequence does not align with block number.");
      return false;
    }

    if (!validateBlockMatchesProposalRound(
        signedPayload.getPayload(), proposedBlock, bftBlockInterface)) {
      return false;
    }

    return true;
  }

  private boolean validateBlockMatchesProposalRound(
      final ProposalPayload payload, final Block block, final BftBlockInterface bftBlockInterface) {
    final ConsensusRoundIdentifier msgRound = payload.getRoundIdentifier();
    final BftExtraData extraData = bftBlockInterface.getExtraData(block.getHeader());
    if (extraData.getRound() != msgRound.getRoundNumber()) {
      LOG.info("Invalid Proposal message, round number in block does not match that in message.");
      return false;
    }
    return true;
  }
}
