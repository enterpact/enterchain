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

import static org.assertj.core.api.Assertions.assertThat;

import org.enterchain.enter.consensus.common.bft.ConsensusRoundIdentifier;
import org.enterchain.enter.consensus.ibft.messagedata.IbftV2;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.rlp.BytesValueRLPOutput;
import org.enterchain.enter.ethereum.rlp.RLP;
import org.enterchain.enter.ethereum.rlp.RLPInput;

import org.apache.tuweni.bytes.Bytes;
import org.junit.Test;

public class PreparePayloadTest {

  @Test
  public void roundTripRlp() {
    final Hash digest = Hash.hash(Bytes.of(1));
    final ConsensusRoundIdentifier expectedRoundIdentifier = new ConsensusRoundIdentifier(1, 1);
    final PreparePayload preparePayload = new PreparePayload(expectedRoundIdentifier, digest);
    final BytesValueRLPOutput rlpOutput = new BytesValueRLPOutput();
    preparePayload.writeTo(rlpOutput);

    final RLPInput rlpInput = RLP.input(rlpOutput.encoded());
    final PreparePayload actualPreparePayload = PreparePayload.readFrom(rlpInput);

    final ConsensusRoundIdentifier actualConsensusRoundIdentifier =
        actualPreparePayload.getRoundIdentifier();
    final Hash actualDigest = actualPreparePayload.getDigest();
    assertThat(actualConsensusRoundIdentifier)
        .isEqualToComparingFieldByField(expectedRoundIdentifier);
    assertThat(actualDigest).isEqualTo(digest);
    assertThat(actualPreparePayload.getMessageType()).isEqualTo(IbftV2.PREPARE);
  }
}
