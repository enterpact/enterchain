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
package org.enterchain.enter.consensus.qbft;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.enterchain.enter.consensus.common.bft.BftContextBuilder.setupContextWithBftExtraDataEncoder;

import org.enterchain.enter.consensus.common.bft.BftBlockHeaderFunctions;
import org.enterchain.enter.consensus.common.bft.BftExtraData;
import org.enterchain.enter.consensus.common.bft.BftExtraDataCodec;
import org.enterchain.enter.consensus.common.bft.BftExtraDataFixture;
import org.enterchain.enter.consensus.common.bft.Vote;
import org.enterchain.enter.crypto.NodeKey;
import org.enterchain.enter.crypto.NodeKeyUtils;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.BlockHeaderTestFixture;
import org.enterchain.enter.ethereum.core.Difficulty;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.core.Util;
import org.enterchain.enter.ethereum.mainnet.BlockHeaderValidator;
import org.enterchain.enter.ethereum.mainnet.HeaderValidationMode;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.junit.Test;

public class QbftBlockHeaderValidationRulesetFactoryTest {

  private ProtocolContext protocolContext(final Collection<Address> validators) {
    return new ProtocolContext(
        null, null, setupContextWithBftExtraDataEncoder(validators, new QbftExtraDataCodec()));
  }

  @Test
  public void bftValidateHeaderPasses() {
    final NodeKey proposerNodeKey = NodeKeyUtils.generate();
    final Address proposerAddress = Util.publicKeyToAddress(proposerNodeKey.getPublicKey());

    final List<Address> validators = singletonList(proposerAddress);

    final BlockHeader parentHeader =
        getPresetHeaderBuilder(1, proposerNodeKey, validators, null).buildHeader();
    final BlockHeader blockHeader =
        getPresetHeaderBuilder(2, proposerNodeKey, validators, parentHeader).buildHeader();

    final BlockHeaderValidator validator =
        QbftBlockHeaderValidationRulesetFactory.blockHeaderValidator(5).build();

    assertThat(
            validator.validateHeader(
                blockHeader, parentHeader, protocolContext(validators), HeaderValidationMode.FULL))
        .isTrue();
  }

  @Test
  public void bftValidateHeaderFailsWhenExtraDataDoesntContainValidatorList() {
    final NodeKey proposerNodeKey = NodeKeyUtils.generate();
    final Address proposerAddress = Util.publicKeyToAddress(proposerNodeKey.getPublicKey());

    final List<Address> validators = singletonList(proposerAddress);

    final BlockHeader parentHeader =
        getPresetHeaderBuilder(1, proposerNodeKey, validators, null).buildHeader();
    final BlockHeader blockHeader =
        getPresetHeaderBuilder(2, proposerNodeKey, emptyList(), parentHeader).buildHeader();

    final BlockHeaderValidator validator =
        QbftBlockHeaderValidationRulesetFactory.blockHeaderValidator(5).build();

    assertThat(
            validator.validateHeader(
                blockHeader, parentHeader, protocolContext(validators), HeaderValidationMode.FULL))
        .isFalse();
  }

  @Test
  public void bftValidateHeaderFailsOnCoinbaseData() {
    final NodeKey proposerNodeKey = NodeKeyUtils.generate();
    final Address proposerAddress = Util.publicKeyToAddress(proposerNodeKey.getPublicKey());

    final Address nonProposerAddress =
        Util.publicKeyToAddress(NodeKeyUtils.generate().getPublicKey());

    final List<Address> validators = singletonList(proposerAddress);

    final BlockHeader parentHeader =
        getPresetHeaderBuilder(1, proposerNodeKey, validators, null).buildHeader();
    final BlockHeader blockHeader =
        getPresetHeaderBuilder(2, proposerNodeKey, validators, parentHeader)
            .coinbase(nonProposerAddress)
            .buildHeader();

    final BlockHeaderValidator validator =
        QbftBlockHeaderValidationRulesetFactory.blockHeaderValidator(5).build();

    assertThat(
            validator.validateHeader(
                blockHeader, parentHeader, protocolContext(validators), HeaderValidationMode.FULL))
        .isFalse();
  }

  @Test
  public void bftValidateHeaderIgnoresNonceValue() {
    final NodeKey proposerNodeKey = NodeKeyUtils.generate();
    final Address proposerAddress = Util.publicKeyToAddress(proposerNodeKey.getPublicKey());

    final List<Address> validators = singletonList(proposerAddress);

    final BlockHeader parentHeader =
        getPresetHeaderBuilder(1, proposerNodeKey, validators, null).buildHeader();
    final BlockHeader blockHeader =
        getPresetHeaderBuilder(
                2, proposerNodeKey, validators, parentHeader, builder -> builder.nonce(3))
            .buildHeader();

    final BlockHeaderValidator validator =
        QbftBlockHeaderValidationRulesetFactory.blockHeaderValidator(5).build();

    assertThat(
            validator.validateHeader(
                blockHeader, parentHeader, protocolContext(validators), HeaderValidationMode.FULL))
        .isTrue();
  }

  @Test
  public void bftValidateHeaderFailsOnTimestamp() {
    final NodeKey proposerNodeKey = NodeKeyUtils.generate();
    final Address proposerAddress = Util.publicKeyToAddress(proposerNodeKey.getPublicKey());

    final List<Address> validators = singletonList(proposerAddress);

    final BlockHeader parentHeader =
        getPresetHeaderBuilder(1, proposerNodeKey, validators, null).buildHeader();
    final BlockHeader blockHeader =
        getPresetHeaderBuilder(2, proposerNodeKey, validators, parentHeader)
            .timestamp(100)
            .buildHeader();

    final BlockHeaderValidator validator =
        QbftBlockHeaderValidationRulesetFactory.blockHeaderValidator(5).build();

    assertThat(
            validator.validateHeader(
                blockHeader, parentHeader, protocolContext(validators), HeaderValidationMode.FULL))
        .isFalse();
  }

  @Test
  public void bftValidateHeaderIgnoresMixHashValue() {
    final NodeKey proposerNodeKey = NodeKeyUtils.generate();
    final Address proposerAddress = Util.publicKeyToAddress(proposerNodeKey.getPublicKey());

    final List<Address> validators = singletonList(proposerAddress);

    final BlockHeader parentHeader =
        getPresetHeaderBuilder(1, proposerNodeKey, validators, null).buildHeader();
    final BlockHeader blockHeader =
        getPresetHeaderBuilder(
                2,
                proposerNodeKey,
                validators,
                parentHeader,
                builder -> builder.mixHash(Hash.EMPTY_TRIE_HASH))
            .buildHeader();

    final BlockHeaderValidator validator =
        QbftBlockHeaderValidationRulesetFactory.blockHeaderValidator(5).build();

    assertThat(
            validator.validateHeader(
                blockHeader, parentHeader, protocolContext(validators), HeaderValidationMode.FULL))
        .isTrue();
  }

  @Test
  public void bftValidateHeaderIgnoresOmmersValue() {
    final NodeKey proposerNodeKey = NodeKeyUtils.generate();
    final Address proposerAddress = Util.publicKeyToAddress(proposerNodeKey.getPublicKey());

    final List<Address> validators = singletonList(proposerAddress);

    final BlockHeader parentHeader =
        getPresetHeaderBuilder(1, proposerNodeKey, validators, null).buildHeader();
    final BlockHeader blockHeader =
        getPresetHeaderBuilder(
                2,
                proposerNodeKey,
                validators,
                parentHeader,
                builder -> builder.ommersHash(Hash.EMPTY_TRIE_HASH))
            .buildHeader();

    final BlockHeaderValidator validator =
        QbftBlockHeaderValidationRulesetFactory.blockHeaderValidator(5).build();

    assertThat(
            validator.validateHeader(
                blockHeader, parentHeader, protocolContext(validators), HeaderValidationMode.FULL))
        .isTrue();
  }

  @Test
  public void bftValidateHeaderFailsOnDifficulty() {
    final NodeKey proposerNodeKey = NodeKeyUtils.generate();
    final Address proposerAddress = Util.publicKeyToAddress(proposerNodeKey.getPublicKey());

    final List<Address> validators = singletonList(proposerAddress);

    final BlockHeader parentHeader =
        getPresetHeaderBuilder(1, proposerNodeKey, validators, null).buildHeader();
    final BlockHeader blockHeader =
        getPresetHeaderBuilder(2, proposerNodeKey, validators, parentHeader)
            .difficulty(Difficulty.of(5))
            .buildHeader();

    final BlockHeaderValidator validator =
        QbftBlockHeaderValidationRulesetFactory.blockHeaderValidator(5).build();

    assertThat(
            validator.validateHeader(
                blockHeader, parentHeader, protocolContext(validators), HeaderValidationMode.FULL))
        .isFalse();
  }

  @Test
  public void bftValidateHeaderFailsOnAncestor() {
    final NodeKey proposerNodeKey = NodeKeyUtils.generate();
    final Address proposerAddress = Util.publicKeyToAddress(proposerNodeKey.getPublicKey());

    final List<Address> validators = singletonList(proposerAddress);

    final BlockHeader parentHeader =
        getPresetHeaderBuilder(1, proposerNodeKey, validators, null).buildHeader();
    final BlockHeader blockHeader =
        getPresetHeaderBuilder(2, proposerNodeKey, validators, null).buildHeader();

    final BlockHeaderValidator validator =
        QbftBlockHeaderValidationRulesetFactory.blockHeaderValidator(5).build();

    assertThat(
            validator.validateHeader(
                blockHeader, parentHeader, protocolContext(validators), HeaderValidationMode.FULL))
        .isFalse();
  }

  @Test
  public void bftValidateHeaderFailsOnGasUsage() {
    final NodeKey proposerNodeKey = NodeKeyUtils.generate();
    final Address proposerAddress = Util.publicKeyToAddress(proposerNodeKey.getPublicKey());

    final List<Address> validators = singletonList(proposerAddress);

    final BlockHeader parentHeader =
        getPresetHeaderBuilder(1, proposerNodeKey, validators, null).buildHeader();
    final BlockHeader blockHeader =
        getPresetHeaderBuilder(2, proposerNodeKey, validators, parentHeader)
            .gasLimit(5_000)
            .gasUsed(6_000)
            .buildHeader();

    final BlockHeaderValidator validator =
        QbftBlockHeaderValidationRulesetFactory.blockHeaderValidator(5).build();

    assertThat(
            validator.validateHeader(
                blockHeader, parentHeader, protocolContext(validators), HeaderValidationMode.FULL))
        .isFalse();
  }

  @Test
  public void bftValidateHeaderFailsOnGasLimitRange() {
    final NodeKey proposerNodeKey = NodeKeyUtils.generate();
    final Address proposerAddress = Util.publicKeyToAddress(proposerNodeKey.getPublicKey());

    final List<Address> validators = singletonList(proposerAddress);

    final BlockHeader parentHeader =
        getPresetHeaderBuilder(1, proposerNodeKey, validators, null).buildHeader();
    final BlockHeader blockHeader =
        getPresetHeaderBuilder(2, proposerNodeKey, validators, parentHeader)
            .gasLimit(4999)
            .buildHeader();

    final BlockHeaderValidator validator =
        QbftBlockHeaderValidationRulesetFactory.blockHeaderValidator(5).build();

    assertThat(
            validator.validateHeader(
                blockHeader, parentHeader, protocolContext(validators), HeaderValidationMode.FULL))
        .isFalse();
  }

  private BlockHeaderTestFixture getPresetHeaderBuilder(
      final long number,
      final NodeKey proposerNodeKey,
      final List<Address> validators,
      final BlockHeader parent) {
    return getPresetHeaderBuilder(number, proposerNodeKey, validators, parent, null);
  }

  private BlockHeaderTestFixture getPresetHeaderBuilder(
      final long number,
      final NodeKey proposerNodeKey,
      final List<Address> validators,
      final BlockHeader parent,
      final HeaderModifier modifier) {
    final BlockHeaderTestFixture builder = new BlockHeaderTestFixture();
    final QbftExtraDataCodec qbftExtraDataEncoder = new QbftExtraDataCodec();

    if (parent != null) {
      builder.parentHash(parent.getHash());
    }
    builder.number(number);
    builder.gasLimit(5000);
    builder.timestamp(6000 * number);
    builder.difficulty(Difficulty.ONE);
    builder.coinbase(Util.publicKeyToAddress(proposerNodeKey.getPublicKey()));
    builder.blockHeaderFunctions(BftBlockHeaderFunctions.forCommittedSeal(qbftExtraDataEncoder));

    if (modifier != null) {
      modifier.update(builder);
    }

    final BftExtraData bftExtraData =
        BftExtraDataFixture.createExtraData(
            builder.buildHeader(),
            Bytes.wrap(new byte[BftExtraDataCodec.EXTRA_VANITY_LENGTH]),
            Optional.of(Vote.authVote(Address.fromHexString("1"))),
            validators,
            singletonList(proposerNodeKey),
            0x2A,
            qbftExtraDataEncoder);

    builder.extraData(qbftExtraDataEncoder.encode(bftExtraData));
    return builder;
  }

  @FunctionalInterface
  public interface HeaderModifier {

    void update(BlockHeaderTestFixture blockHeaderTestFixture);
  }
}
