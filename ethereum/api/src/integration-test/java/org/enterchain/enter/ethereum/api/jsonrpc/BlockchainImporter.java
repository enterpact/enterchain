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
package org.enterchain.enter.ethereum.api.jsonrpc;

import org.enterchain.enter.config.GenesisConfigFile;
import org.enterchain.enter.ethereum.chain.GenesisState;
import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.mainnet.MainnetProtocolSchedule;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.mainnet.ScheduleBasedBlockHeaderFunctions;
import org.enterchain.enter.ethereum.util.RawBlockIterator;

import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Creates a block chain from a genesis and a blocks files. */
public class BlockchainImporter {

  private final GenesisState genesisState;

  private final ProtocolSchedule protocolSchedule;

  private final List<Block> blocks;

  private final Block genesisBlock;

  public BlockchainImporter(final URL blocksUrl, final String genesisJson) throws Exception {
    protocolSchedule =
        MainnetProtocolSchedule.fromConfig(
            GenesisConfigFile.fromConfig(genesisJson).getConfigOptions());

    blocks = new ArrayList<>();
    try (final RawBlockIterator iterator =
        new RawBlockIterator(
            Paths.get(blocksUrl.toURI()),
            rlp ->
                BlockHeader.readFrom(
                    rlp, ScheduleBasedBlockHeaderFunctions.create(protocolSchedule)))) {
      while (iterator.hasNext()) {
        blocks.add(iterator.next());
      }
    }

    genesisBlock = blocks.get(0);
    genesisState = GenesisState.fromJson(genesisJson, protocolSchedule);
  }

  public GenesisState getGenesisState() {
    return genesisState;
  }

  public ProtocolSchedule getProtocolSchedule() {
    return protocolSchedule;
  }

  public List<Block> getBlocks() {
    return blocks;
  }

  public Block getGenesisBlock() {
    return genesisBlock;
  }
}
