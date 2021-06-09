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

import org.enterchain.enter.consensus.common.bft.Gossiper;
import org.enterchain.enter.consensus.common.bft.network.ValidatorMulticaster;
import org.enterchain.enter.consensus.common.bft.payload.Authored;
import org.enterchain.enter.consensus.qbft.messagedata.CommitMessageData;
import org.enterchain.enter.consensus.qbft.messagedata.PrepareMessageData;
import org.enterchain.enter.consensus.qbft.messagedata.ProposalMessageData;
import org.enterchain.enter.consensus.qbft.messagedata.QbftV1;
import org.enterchain.enter.consensus.qbft.messagedata.RoundChangeMessageData;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.p2p.rlpx.wire.Message;
import org.enterchain.enter.ethereum.p2p.rlpx.wire.MessageData;

import java.util.List;

import com.google.common.collect.Lists;

/** Class responsible for rebroadcasting IBFT messages to known validators */
public class QbftGossip implements Gossiper {

  private final ValidatorMulticaster multicaster;

  /**
   * Constructor that attaches gossip logic to a set of multicaster
   *
   * @param multicaster Network connections to the remote validators
   */
  public QbftGossip(final ValidatorMulticaster multicaster) {
    this.multicaster = multicaster;
  }

  /**
   * Retransmit a given IBFT message to other known validators nodes
   *
   * @param message The raw message to be gossiped
   */
  @Override
  public void send(final Message message) {
    final MessageData messageData = message.getData();
    final Authored decodedMessage;
    switch (messageData.getCode()) {
      case QbftV1.PROPOSAL:
        decodedMessage = ProposalMessageData.fromMessageData(messageData).decode();
        break;
      case QbftV1.PREPARE:
        decodedMessage = PrepareMessageData.fromMessageData(messageData).decode();
        break;
      case QbftV1.COMMIT:
        decodedMessage = CommitMessageData.fromMessageData(messageData).decode();
        break;
      case QbftV1.ROUND_CHANGE:
        decodedMessage = RoundChangeMessageData.fromMessageData(messageData).decode();
        break;
      default:
        throw new IllegalArgumentException(
            "Received message does not conform to any recognised IBFT message structure.");
    }
    final List<Address> excludeAddressesList =
        Lists.newArrayList(
            message.getConnection().getPeerInfo().getAddress(), decodedMessage.getAuthor());

    multicaster.send(messageData, excludeAddressesList);
  }
}
