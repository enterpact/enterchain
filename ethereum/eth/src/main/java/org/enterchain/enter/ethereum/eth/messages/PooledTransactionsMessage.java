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
package org.enterchain.enter.ethereum.eth.messages;

import org.enterchain.enter.ethereum.core.Transaction;
import org.enterchain.enter.ethereum.p2p.rlpx.wire.AbstractMessageData;
import org.enterchain.enter.ethereum.p2p.rlpx.wire.MessageData;
import org.enterchain.enter.ethereum.rlp.BytesValueRLPInput;
import org.enterchain.enter.ethereum.rlp.BytesValueRLPOutput;

import java.util.List;

import org.apache.tuweni.bytes.Bytes;

public final class PooledTransactionsMessage extends AbstractMessageData {

  private static final int MESSAGE_CODE = EthPV65.POOLED_TRANSACTIONS;
  private List<Transaction> pooledTransactions;

  private PooledTransactionsMessage(final Bytes rlp) {
    super(rlp);
  }

  @Override
  public int getCode() {
    return MESSAGE_CODE;
  }

  public static PooledTransactionsMessage create(final List<Transaction> transactions) {
    List<Transaction> tx = transactions;
    final BytesValueRLPOutput out = new BytesValueRLPOutput();
    out.writeList(tx, Transaction::writeTo);
    return new PooledTransactionsMessage(out.encoded());
  }

  public static PooledTransactionsMessage readFrom(final MessageData message) {
    if (message instanceof PooledTransactionsMessage) {
      return (PooledTransactionsMessage) message;
    }
    final int code = message.getCode();
    if (code != MESSAGE_CODE) {
      throw new IllegalArgumentException(
          String.format("Message has code %d and thus is not a PooledTransactionsMessage.", code));
    }

    return new PooledTransactionsMessage(message.getData());
  }

  public List<Transaction> transactions() {
    if (pooledTransactions == null) {
      final BytesValueRLPInput in = new BytesValueRLPInput(getData(), false);
      pooledTransactions = in.readList(Transaction::readFrom);
    }
    return pooledTransactions;
  }
}
