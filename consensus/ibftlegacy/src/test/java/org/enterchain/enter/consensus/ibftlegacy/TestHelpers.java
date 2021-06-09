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
package org.enterchain.enter.consensus.ibftlegacy;

import org.enterchain.enter.crypto.KeyPair;
import org.enterchain.enter.crypto.SECPSignature;
import org.enterchain.enter.crypto.SignatureAlgorithmFactory;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.BlockHeaderTestFixture;
import org.enterchain.enter.ethereum.core.Hash;

import java.util.Collections;
import java.util.List;

import org.apache.tuweni.bytes.Bytes;

public class TestHelpers {

  public static BlockHeader createIbftSignedBlockHeader(
      final BlockHeaderTestFixture blockHeaderBuilder,
      final KeyPair signer,
      final List<Address> validators) {

    final IbftExtraData unsignedExtraData =
        new IbftExtraData(Bytes.wrap(new byte[32]), Collections.emptyList(), null, validators);
    blockHeaderBuilder.extraData(unsignedExtraData.encode());

    final Hash signingHash =
        IbftBlockHashing.calculateDataHashForProposerSeal(
            blockHeaderBuilder.buildHeader(), unsignedExtraData);

    final SECPSignature proposerSignature =
        SignatureAlgorithmFactory.getInstance().sign(signingHash, signer);

    final IbftExtraData signedExtraData =
        new IbftExtraData(
            unsignedExtraData.getVanityData(),
            unsignedExtraData.getSeals(),
            proposerSignature,
            unsignedExtraData.getValidators());

    blockHeaderBuilder.extraData(signedExtraData.encode());

    return blockHeaderBuilder.buildHeader();
  }
}