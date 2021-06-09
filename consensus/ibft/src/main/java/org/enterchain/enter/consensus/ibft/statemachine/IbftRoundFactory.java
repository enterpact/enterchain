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
package org.enterchain.enter.consensus.ibft.statemachine;

import org.enterchain.enter.consensus.common.bft.BftExtraDataCodec;
import org.enterchain.enter.consensus.common.bft.ConsensusRoundIdentifier;
import org.enterchain.enter.consensus.common.bft.blockcreation.BftBlockCreator;
import org.enterchain.enter.consensus.common.bft.blockcreation.BftBlockCreatorFactory;
import org.enterchain.enter.consensus.common.bft.statemachine.BftFinalState;
import org.enterchain.enter.consensus.ibft.network.IbftMessageTransmitter;
import org.enterchain.enter.consensus.ibft.payload.MessageFactory;
import org.enterchain.enter.consensus.ibft.validation.MessageValidatorFactory;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.chain.MinedBlockObserver;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.util.Subscribers;

public class IbftRoundFactory {

  private final BftFinalState finalState;
  private final BftBlockCreatorFactory blockCreatorFactory;
  private final ProtocolContext protocolContext;
  private final ProtocolSchedule protocolSchedule;
  private final Subscribers<MinedBlockObserver> minedBlockObservers;
  private final MessageValidatorFactory messageValidatorFactory;
  private final MessageFactory messageFactory;
  private final BftExtraDataCodec bftExtraDataCodec;

  public IbftRoundFactory(
      final BftFinalState finalState,
      final ProtocolContext protocolContext,
      final ProtocolSchedule protocolSchedule,
      final Subscribers<MinedBlockObserver> minedBlockObservers,
      final MessageValidatorFactory messageValidatorFactory,
      final MessageFactory messageFactory,
      final BftExtraDataCodec bftExtraDataCodec) {
    this.finalState = finalState;
    this.blockCreatorFactory = finalState.getBlockCreatorFactory();
    this.protocolContext = protocolContext;
    this.protocolSchedule = protocolSchedule;
    this.minedBlockObservers = minedBlockObservers;
    this.messageValidatorFactory = messageValidatorFactory;
    this.messageFactory = messageFactory;
    this.bftExtraDataCodec = bftExtraDataCodec;
  }

  public IbftRound createNewRound(final BlockHeader parentHeader, final int round) {
    long nextBlockHeight = parentHeader.getNumber() + 1;
    final ConsensusRoundIdentifier roundIdentifier =
        new ConsensusRoundIdentifier(nextBlockHeight, round);

    final RoundState roundState =
        new RoundState(
            roundIdentifier,
            finalState.getQuorum(),
            messageValidatorFactory.createMessageValidator(roundIdentifier, parentHeader));

    return createNewRoundWithState(parentHeader, roundState);
  }

  public IbftRound createNewRoundWithState(
      final BlockHeader parentHeader, final RoundState roundState) {
    final ConsensusRoundIdentifier roundIdentifier = roundState.getRoundIdentifier();
    final BftBlockCreator blockCreator =
        blockCreatorFactory.create(parentHeader, roundIdentifier.getRoundNumber());

    final IbftMessageTransmitter messageTransmitter =
        new IbftMessageTransmitter(messageFactory, finalState.getValidatorMulticaster());

    return new IbftRound(
        roundState,
        blockCreator,
        protocolContext,
        protocolSchedule.getByBlockNumber(roundIdentifier.getSequenceNumber()).getBlockImporter(),
        minedBlockObservers,
        finalState.getNodeKey(),
        messageFactory,
        messageTransmitter,
        finalState.getRoundTimer(),
        bftExtraDataCodec);
  }
}
