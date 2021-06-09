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
package org.enterchain.enter.consensus.qbft.statemachine;

import org.enterchain.enter.consensus.common.bft.Gossiper;
import org.enterchain.enter.consensus.common.bft.MessageTracker;
import org.enterchain.enter.consensus.common.bft.SynchronizerUpdater;
import org.enterchain.enter.consensus.common.bft.statemachine.BaseBftController;
import org.enterchain.enter.consensus.common.bft.statemachine.BaseBlockHeightManager;
import org.enterchain.enter.consensus.common.bft.statemachine.BftFinalState;
import org.enterchain.enter.consensus.common.bft.statemachine.FutureMessageBuffer;
import org.enterchain.enter.consensus.qbft.messagedata.CommitMessageData;
import org.enterchain.enter.consensus.qbft.messagedata.PrepareMessageData;
import org.enterchain.enter.consensus.qbft.messagedata.ProposalMessageData;
import org.enterchain.enter.consensus.qbft.messagedata.QbftV1;
import org.enterchain.enter.consensus.qbft.messagedata.RoundChangeMessageData;
import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.p2p.rlpx.wire.Message;
import org.enterchain.enter.ethereum.p2p.rlpx.wire.MessageData;

public class QbftController extends BaseBftController {

  private BaseQbftBlockHeightManager currentHeightManager;
  private final QbftBlockHeightManagerFactory qbftBlockHeightManagerFactory;

  public QbftController(
      final Blockchain blockchain,
      final BftFinalState bftFinalState,
      final QbftBlockHeightManagerFactory qbftBlockHeightManagerFactory,
      final Gossiper gossiper,
      final MessageTracker duplicateMessageTracker,
      final FutureMessageBuffer futureMessageBuffer,
      final SynchronizerUpdater sychronizerUpdater) {

    super(
        blockchain,
        bftFinalState,
        gossiper,
        duplicateMessageTracker,
        futureMessageBuffer,
        sychronizerUpdater);
    this.qbftBlockHeightManagerFactory = qbftBlockHeightManagerFactory;
  }

  @Override
  protected void handleMessage(final Message message) {
    final MessageData messageData = message.getData();

    switch (messageData.getCode()) {
      case QbftV1.PROPOSAL:
        consumeMessage(
            message,
            ProposalMessageData.fromMessageData(messageData).decode(),
            currentHeightManager::handleProposalPayload);
        break;

      case QbftV1.PREPARE:
        consumeMessage(
            message,
            PrepareMessageData.fromMessageData(messageData).decode(),
            currentHeightManager::handlePreparePayload);
        break;

      case QbftV1.COMMIT:
        consumeMessage(
            message,
            CommitMessageData.fromMessageData(messageData).decode(),
            currentHeightManager::handleCommitPayload);
        break;

      case QbftV1.ROUND_CHANGE:
        consumeMessage(
            message,
            RoundChangeMessageData.fromMessageData(messageData).decode(),
            currentHeightManager::handleRoundChangePayload);
        break;

      default:
        throw new IllegalArgumentException(
            String.format(
                "Received message with messageCode=%d does not conform to any recognised QBFT message structure",
                message.getData().getCode()));
    }
  }

  @Override
  protected void createNewHeightManager(final BlockHeader parentHeader) {
    currentHeightManager = qbftBlockHeightManagerFactory.create(parentHeader);
  }

  @Override
  protected BaseBlockHeightManager getCurrentHeightManager() {
    return currentHeightManager;
  }
}
