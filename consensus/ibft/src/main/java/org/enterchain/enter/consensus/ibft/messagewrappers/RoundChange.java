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
package org.enterchain.enter.consensus.ibft.messagewrappers;

import org.enterchain.enter.consensus.common.bft.BftBlockHeaderFunctions;
import org.enterchain.enter.consensus.common.bft.ConsensusRoundIdentifier;
import org.enterchain.enter.consensus.common.bft.messagewrappers.BftMessage;
import org.enterchain.enter.consensus.common.bft.payload.SignedData;
import org.enterchain.enter.consensus.ibft.IbftExtraDataCodec;
import org.enterchain.enter.consensus.ibft.payload.PayloadDeserializers;
import org.enterchain.enter.consensus.ibft.payload.PreparedCertificate;
import org.enterchain.enter.consensus.ibft.payload.RoundChangePayload;
import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.rlp.BytesValueRLPOutput;
import org.enterchain.enter.ethereum.rlp.RLP;
import org.enterchain.enter.ethereum.rlp.RLPInput;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;

public class RoundChange extends BftMessage<RoundChangePayload> {

  private static final IbftExtraDataCodec BFT_EXTRA_DATA_ENCODER = new IbftExtraDataCodec();
  private final Optional<Block> proposedBlock;

  public RoundChange(
      final SignedData<RoundChangePayload> payload, final Optional<Block> proposedBlock) {
    super(payload);
    this.proposedBlock = proposedBlock;
  }

  public Optional<Block> getProposedBlock() {
    return proposedBlock;
  }

  public Optional<PreparedCertificate> getPreparedCertificate() {
    return getPayload().getPreparedCertificate();
  }

  public Optional<ConsensusRoundIdentifier> getPreparedCertificateRound() {
    return getPreparedCertificate()
        .map(prepCert -> prepCert.getProposalPayload().getPayload().getRoundIdentifier());
  }

  @Override
  public Bytes encode() {
    final BytesValueRLPOutput rlpOut = new BytesValueRLPOutput();
    rlpOut.startList();
    getSignedPayload().writeTo(rlpOut);
    if (proposedBlock.isPresent()) {
      proposedBlock.get().writeTo(rlpOut);
    } else {
      rlpOut.writeNull();
    }
    rlpOut.endList();
    return rlpOut.encoded();
  }

  public static RoundChange decode(final Bytes data) {

    final RLPInput rlpIn = RLP.input(data);
    rlpIn.enterList();
    final SignedData<RoundChangePayload> payload =
        PayloadDeserializers.readSignedRoundChangePayloadFrom(rlpIn);
    Optional<Block> block = Optional.empty();
    if (!rlpIn.nextIsNull()) {
      block =
          Optional.of(
              Block.readFrom(
                  rlpIn, BftBlockHeaderFunctions.forCommittedSeal(BFT_EXTRA_DATA_ENCODER)));
    } else {
      rlpIn.skipNext();
    }
    rlpIn.leaveList();

    return new RoundChange(payload, block);
  }
}
