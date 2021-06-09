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
package org.enterchain.enter.consensus.qbt.support;

import org.enterchain.enter.consensus.common.bft.ConsensusRoundIdentifier;
import org.enterchain.enter.consensus.common.bft.messagewrappers.BftMessage;
import org.enterchain.enter.consensus.common.bft.payload.SignedData;
import org.enterchain.enter.consensus.qbft.messagewrappers.Commit;
import org.enterchain.enter.consensus.qbft.payload.CommitPayload;
import org.enterchain.enter.crypto.SignatureAlgorithm;
import org.enterchain.enter.crypto.SignatureAlgorithmFactory;
import org.enterchain.enter.ethereum.core.Hash;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.apache.tuweni.bytes.Bytes;

public class CommitMessage implements RlpTestCaseMessage {
  private static final Supplier<SignatureAlgorithm> SIGNATURE_ALGORITHM =
      Suppliers.memoize(SignatureAlgorithmFactory::getInstance);
  private final UnsignedCommit unsignedCommit;
  private final String signature;

  @JsonCreator
  public CommitMessage(
      @JsonProperty("unsignedCommit") final UnsignedCommit unsignedCommit,
      @JsonProperty("signature") final String signature) {
    this.unsignedCommit = unsignedCommit;
    this.signature = signature;
  }

  @Override
  public BftMessage<CommitPayload> fromRlp(final Bytes rlp) {
    return Commit.decode(rlp);
  }

  @Override
  public BftMessage<CommitPayload> toBftMessage() {
    final CommitPayload commitPayload =
        new CommitPayload(
            new ConsensusRoundIdentifier(unsignedCommit.sequence, unsignedCommit.round),
            Hash.fromHexStringLenient(unsignedCommit.digest),
            SIGNATURE_ALGORITHM
                .get()
                .decodeSignature(Bytes.fromHexString(unsignedCommit.commitSeal)));
    final SignedData<CommitPayload> signedCommitPayload =
        SignedData.create(
            commitPayload,
            SIGNATURE_ALGORITHM.get().decodeSignature(Bytes.fromHexString(signature)));
    return new Commit(signedCommitPayload);
  }

  public static class UnsignedCommit {
    private final long sequence;
    private final int round;
    private final String commitSeal;
    private final String digest;

    @JsonCreator
    public UnsignedCommit(
        @JsonProperty("sequence") final long sequence,
        @JsonProperty("round") final int round,
        @JsonProperty("commitSeal") final String commitSeal,
        @JsonProperty("digest") final String digest) {
      this.sequence = sequence;
      this.round = round;
      this.commitSeal = commitSeal;
      this.digest = digest;
    }
  }
}
