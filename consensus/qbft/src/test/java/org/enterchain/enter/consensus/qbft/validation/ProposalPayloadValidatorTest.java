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
package org.enterchain.enter.consensus.qbft.validation;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.enterchain.enter.consensus.common.bft.BftContextBuilder.setupContextWithBftExtraDataEncoder;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.enterchain.enter.consensus.common.bft.ConsensusRoundHelpers;
import org.enterchain.enter.consensus.common.bft.ConsensusRoundIdentifier;
import org.enterchain.enter.consensus.common.bft.ProposedBlockHelpers;
import org.enterchain.enter.consensus.qbft.QbftExtraDataCodec;
import org.enterchain.enter.consensus.qbft.messagewrappers.Proposal;
import org.enterchain.enter.consensus.qbft.payload.MessageFactory;
import org.enterchain.enter.crypto.NodeKey;
import org.enterchain.enter.crypto.NodeKeyUtils;
import org.enterchain.enter.ethereum.BlockValidator;
import org.enterchain.enter.ethereum.BlockValidator.BlockProcessingOutputs;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.chain.MutableBlockchain;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.Util;
import org.enterchain.enter.ethereum.mainnet.HeaderValidationMode;
import org.enterchain.enter.ethereum.worldstate.WorldStateArchive;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProposalPayloadValidatorTest {

  @Mock private BlockValidator blockValidator;
  @Mock private MutableBlockchain blockChain;
  @Mock private WorldStateArchive worldStateArchive;
  private ProtocolContext protocolContext;

  private static final int CHAIN_HEIGHT = 3;
  private final ConsensusRoundIdentifier targetRound =
      new ConsensusRoundIdentifier(CHAIN_HEIGHT, 3);

  private final NodeKey nodeKey = NodeKeyUtils.generate();
  private final Address expectedProposer = Util.publicKeyToAddress(nodeKey.getPublicKey());
  private final MessageFactory messageFactory = new MessageFactory(nodeKey);
  final ConsensusRoundIdentifier roundIdentifier =
      ConsensusRoundHelpers.createFrom(targetRound, 1, 0);
  final QbftExtraDataCodec bftExtraDataEncoder = new QbftExtraDataCodec();

  @Before
  public void setup() {
    protocolContext =
        new ProtocolContext(
            blockChain,
            worldStateArchive,
            setupContextWithBftExtraDataEncoder(emptyList(), bftExtraDataEncoder));
  }

  @Test
  public void validationPassesWhenProposerAndRoundMatchAndBlockIsValid() {
    final ProposalPayloadValidator payloadValidator =
        new ProposalPayloadValidator(
            expectedProposer, roundIdentifier, blockValidator, protocolContext);
    final Block block =
        ProposedBlockHelpers.createProposalBlock(emptyList(), roundIdentifier, bftExtraDataEncoder);
    final Proposal proposal =
        messageFactory.createProposal(roundIdentifier, block, emptyList(), emptyList());

    when(blockValidator.validateAndProcessBlock(
            eq(protocolContext),
            eq(block),
            eq(HeaderValidationMode.LIGHT),
            eq(HeaderValidationMode.FULL)))
        .thenReturn(Optional.of(new BlockProcessingOutputs(null, null)));

    assertThat(payloadValidator.validate(proposal.getSignedPayload())).isTrue();
  }

  @Test
  public void validationPassesWhenBlockRoundDoesNotMatchProposalRound() {
    final ProposalPayloadValidator payloadValidator =
        new ProposalPayloadValidator(
            expectedProposer, roundIdentifier, blockValidator, protocolContext);

    final Block block =
        ProposedBlockHelpers.createProposalBlock(
            emptyList(),
            ConsensusRoundHelpers.createFrom(roundIdentifier, 0, +1),
            bftExtraDataEncoder);
    final Proposal proposal =
        messageFactory.createProposal(roundIdentifier, block, emptyList(), emptyList());

    when(blockValidator.validateAndProcessBlock(
            eq(protocolContext),
            eq(block),
            eq(HeaderValidationMode.LIGHT),
            eq(HeaderValidationMode.FULL)))
        .thenReturn(Optional.of(new BlockProcessingOutputs(null, null)));

    assertThat(payloadValidator.validate(proposal.getSignedPayload())).isTrue();
  }

  @Test
  public void validationFailsWhenBlockFailsValidation() {
    final ConsensusRoundIdentifier roundIdentifier =
        ConsensusRoundHelpers.createFrom(targetRound, 1, 0);

    final ProposalPayloadValidator payloadValidator =
        new ProposalPayloadValidator(
            expectedProposer, roundIdentifier, blockValidator, protocolContext);
    final Block block =
        ProposedBlockHelpers.createProposalBlock(emptyList(), roundIdentifier, bftExtraDataEncoder);
    final Proposal proposal =
        messageFactory.createProposal(roundIdentifier, block, emptyList(), emptyList());

    when(blockValidator.validateAndProcessBlock(
            eq(protocolContext),
            eq(block),
            eq(HeaderValidationMode.LIGHT),
            eq(HeaderValidationMode.FULL)))
        .thenReturn(Optional.empty());

    assertThat(payloadValidator.validate(proposal.getSignedPayload())).isFalse();
  }

  @Test
  public void validationFailsWhenExpectedProposerDoesNotMatchPayloadsAuthor() {
    final ProposalPayloadValidator payloadValidator =
        new ProposalPayloadValidator(
            Address.fromHexString("0x1"), roundIdentifier, blockValidator, protocolContext);
    final Block block = ProposedBlockHelpers.createProposalBlock(emptyList(), roundIdentifier);
    final Proposal proposal =
        messageFactory.createProposal(roundIdentifier, block, emptyList(), emptyList());

    assertThat(payloadValidator.validate(proposal.getSignedPayload())).isFalse();
    verifyNoMoreInteractions(blockValidator);
  }

  @Test
  public void validationFailsWhenMessageMismatchesExpectedRound() {
    final ProposalPayloadValidator payloadValidator =
        new ProposalPayloadValidator(
            expectedProposer, roundIdentifier, blockValidator, protocolContext);

    final Block block = ProposedBlockHelpers.createProposalBlock(emptyList(), roundIdentifier);
    final Proposal proposal =
        messageFactory.createProposal(
            ConsensusRoundHelpers.createFrom(roundIdentifier, 0, +1),
            block,
            emptyList(),
            emptyList());

    assertThat(payloadValidator.validate(proposal.getSignedPayload())).isFalse();
    verifyNoMoreInteractions(blockValidator);
  }

  @Test
  public void validationFailsWhenMessageMismatchesExpectedHeight() {
    final ProposalPayloadValidator payloadValidator =
        new ProposalPayloadValidator(
            expectedProposer, roundIdentifier, blockValidator, protocolContext);

    final Block block = ProposedBlockHelpers.createProposalBlock(emptyList(), roundIdentifier);
    final Proposal proposal =
        messageFactory.createProposal(
            ConsensusRoundHelpers.createFrom(roundIdentifier, +1, 0),
            block,
            emptyList(),
            emptyList());

    assertThat(payloadValidator.validate(proposal.getSignedPayload())).isFalse();
    verifyNoMoreInteractions(blockValidator);
  }

  @Test
  public void validationFailsForBlockWithIncorrectHeight() {
    final ProposalPayloadValidator payloadValidator =
        new ProposalPayloadValidator(
            expectedProposer, roundIdentifier, blockValidator, protocolContext);
    final Block block =
        ProposedBlockHelpers.createProposalBlock(
            emptyList(),
            ConsensusRoundHelpers.createFrom(roundIdentifier, +1, 0),
            bftExtraDataEncoder);
    final Proposal proposal =
        messageFactory.createProposal(roundIdentifier, block, emptyList(), emptyList());

    when(blockValidator.validateAndProcessBlock(
            eq(protocolContext),
            eq(block),
            eq(HeaderValidationMode.LIGHT),
            eq(HeaderValidationMode.FULL)))
        .thenReturn(Optional.of(new BlockProcessingOutputs(null, null)));

    assertThat(payloadValidator.validate(proposal.getSignedPayload())).isFalse();
  }
}
