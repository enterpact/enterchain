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

import org.enterchain.enter.config.GenesisConfigFile;
import org.enterchain.enter.ethereum.core.BlockBody;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.Transaction;
import org.enterchain.enter.ethereum.difficulty.fixed.FixedDifficultyProtocolSchedule;
import org.enterchain.enter.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.enterchain.enter.ethereum.p2p.rlpx.wire.MessageData;
import org.enterchain.enter.ethereum.p2p.rlpx.wire.RawMessage;
import org.enterchain.enter.ethereum.rlp.BytesValueRLPInput;
import org.enterchain.enter.ethereum.rlp.RLP;
import org.enterchain.enter.ethereum.rlp.RLPInput;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.common.io.Resources;
import org.apache.tuweni.bytes.Bytes;
import org.assertj.core.api.Assertions;
import org.junit.Test;

/** Tests for {@link BlockBodiesMessage}. */
public final class BlockBodiesMessageTest {

  @Test
  public void blockBodiesRoundTrip() throws IOException {
    final List<BlockBody> bodies = new ArrayList<>();
    final ByteBuffer buffer =
        ByteBuffer.wrap(Resources.toByteArray(this.getClass().getResource("/50.blocks")));
    for (int i = 0; i < 50; ++i) {
      final int blockSize = RLP.calculateSize(Bytes.wrapByteBuffer(buffer));
      final byte[] block = new byte[blockSize];
      buffer.get(block);
      buffer.compact().position(0);
      final RLPInput oneBlock = new BytesValueRLPInput(Bytes.wrap(block), false);
      oneBlock.enterList();
      // We don't care about the header, just the body
      oneBlock.skipNext();
      bodies.add(
          // We know the test data to only contain Frontier blocks
          new BlockBody(
              oneBlock.readList(Transaction::readFrom),
              oneBlock.readList(
                  rlp -> BlockHeader.readFrom(rlp, new MainnetBlockHeaderFunctions()))));
    }
    final MessageData initialMessage = BlockBodiesMessage.create(bodies);
    final MessageData raw = new RawMessage(EthPV62.BLOCK_BODIES, initialMessage.getData());
    final BlockBodiesMessage message = BlockBodiesMessage.readFrom(raw);
    final Iterator<BlockBody> readBodies =
        message
            .bodies(
                FixedDifficultyProtocolSchedule.create(
                    GenesisConfigFile.development().getConfigOptions(), false))
            .iterator();
    for (int i = 0; i < 50; ++i) {
      Assertions.assertThat(readBodies.next()).isEqualTo(bodies.get(i));
    }
  }
}
