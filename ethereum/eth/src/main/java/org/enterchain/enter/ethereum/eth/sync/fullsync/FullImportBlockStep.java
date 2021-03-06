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
package org.enterchain.enter.ethereum.eth.sync.fullsync;

import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.BlockImporter;
import org.enterchain.enter.ethereum.eth.manager.EthContext;
import org.enterchain.enter.ethereum.eth.sync.tasks.exceptions.InvalidBlockException;
import org.enterchain.enter.ethereum.mainnet.HeaderValidationMode;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;

import java.time.Instant;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FullImportBlockStep implements Consumer<Block> {
  private static final Logger LOG = LogManager.getLogger();
  private final ProtocolSchedule protocolSchedule;
  private final ProtocolContext protocolContext;
  private final EthContext ethContext;
  private long gasAccumulator = 0;
  private long lastReportMillis = 0;

  public FullImportBlockStep(
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final EthContext ethContext) {
    this.protocolSchedule = protocolSchedule;
    this.protocolContext = protocolContext;
    this.ethContext = ethContext;
  }

  @Override
  public void accept(final Block block) {
    final long blockNumber = block.getHeader().getNumber();
    final String blockHash = block.getHash().toHexString();
    final BlockImporter importer =
        protocolSchedule.getByBlockNumber(blockNumber).getBlockImporter();
    if (!importer.importBlock(protocolContext, block, HeaderValidationMode.SKIP_DETACHED)) {
      throw new InvalidBlockException("Failed to import block", blockNumber, block.getHash());
    }
    gasAccumulator += block.getHeader().getGasUsed();
    int peerCount = -1; // ethContext is not available in tests
    if (ethContext != null && ethContext.getEthPeers().peerCount() >= 0) {
      peerCount = ethContext.getEthPeers().peerCount();
    }
    if (blockNumber % 200 == 0) {
      final long nowMilli = Instant.now().toEpochMilli();
      final long deltaMilli = nowMilli - lastReportMillis;
      final String mgps =
          (lastReportMillis == 0 || gasAccumulator == 0)
              ? "-"
              : String.format("%.3f", gasAccumulator / 1000.0 / deltaMilli);
      LOG.info(
          "Import reached block {} ({}), {} Mg/s, Peers: {}",
          blockNumber,
          blockHash,
          mgps,
          peerCount);
      lastReportMillis = nowMilli;
      gasAccumulator = 0;
    }
  }
}
