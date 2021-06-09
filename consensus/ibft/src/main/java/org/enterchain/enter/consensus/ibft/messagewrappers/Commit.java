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

import org.enterchain.enter.consensus.common.bft.messagewrappers.BftMessage;
import org.enterchain.enter.consensus.common.bft.payload.SignedData;
import org.enterchain.enter.consensus.ibft.payload.CommitPayload;
import org.enterchain.enter.consensus.ibft.payload.PayloadDeserializers;
import org.enterchain.enter.crypto.SECPSignature;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.rlp.RLP;

import org.apache.tuweni.bytes.Bytes;

public class Commit extends BftMessage<CommitPayload> {

  public Commit(final SignedData<CommitPayload> payload) {
    super(payload);
  }

  public SECPSignature getCommitSeal() {
    return getPayload().getCommitSeal();
  }

  public Hash getDigest() {
    return getPayload().getDigest();
  }

  public static Commit decode(final Bytes data) {
    return new Commit(PayloadDeserializers.readSignedCommitPayloadFrom(RLP.input(data)));
  }
}