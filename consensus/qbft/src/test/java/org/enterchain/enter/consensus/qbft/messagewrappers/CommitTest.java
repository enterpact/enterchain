/*
 * Copyright 2020 ConsenSys AG.
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
package org.enterchain.enter.consensus.qbft.messagewrappers;

import static org.assertj.core.api.Assertions.assertThat;

import org.enterchain.enter.consensus.common.bft.ConsensusRoundIdentifier;
import org.enterchain.enter.consensus.common.bft.payload.SignedData;
import org.enterchain.enter.consensus.qbft.messagedata.QbftV1;
import org.enterchain.enter.consensus.qbft.payload.CommitPayload;
import org.enterchain.enter.crypto.NodeKey;
import org.enterchain.enter.crypto.NodeKeyUtils;
import org.enterchain.enter.crypto.SignatureAlgorithmFactory;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.core.Util;

import java.math.BigInteger;

import org.junit.Test;

public class CommitTest {

  @Test
  public void canRoundTripACommitMessage() {
    final NodeKey nodeKey = NodeKeyUtils.generate();
    final Address addr = Util.publicKeyToAddress(nodeKey.getPublicKey());

    final CommitPayload commitPayload =
        new CommitPayload(
            new ConsensusRoundIdentifier(1, 1),
            Hash.ZERO,
            SignatureAlgorithmFactory.getInstance()
                .createSignature(BigInteger.ONE, BigInteger.ONE, (byte) 0));

    final SignedData<CommitPayload> signedCommitPayload =
        SignedData.create(commitPayload, nodeKey.sign(commitPayload.hashForSignature()));

    final Commit commitMsg = new Commit(signedCommitPayload);

    final Commit decodedPrepare = Commit.decode(commitMsg.encode());

    assertThat(decodedPrepare.getMessageType()).isEqualTo(QbftV1.COMMIT);
    assertThat(decodedPrepare.getAuthor()).isEqualTo(addr);
    assertThat(decodedPrepare.getSignedPayload())
        .isEqualToComparingFieldByField(signedCommitPayload);
  }
}
