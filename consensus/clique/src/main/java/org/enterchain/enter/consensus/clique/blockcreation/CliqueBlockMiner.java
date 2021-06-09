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
package org.enterchain.enter.consensus.clique.blockcreation;

import org.enterchain.enter.consensus.clique.CliqueHelpers;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.blockcreation.AbstractBlockScheduler;
import org.enterchain.enter.ethereum.blockcreation.BlockMiner;
import org.enterchain.enter.ethereum.chain.MinedBlockObserver;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.util.Subscribers;

import java.util.function.Function;

public class CliqueBlockMiner extends BlockMiner<CliqueBlockCreator> {

  private final Address localAddress;

  public CliqueBlockMiner(
      final Function<BlockHeader, CliqueBlockCreator> blockCreator,
      final ProtocolSchedule protocolSchedule,
      final ProtocolContext protocolContext,
      final Subscribers<MinedBlockObserver> observers,
      final AbstractBlockScheduler scheduler,
      final BlockHeader parentHeader,
      final Address localAddress) {
    super(blockCreator, protocolSchedule, protocolContext, observers, scheduler, parentHeader);
    this.localAddress = localAddress;
  }

  @Override
  protected boolean mineBlock() throws InterruptedException {
    if (CliqueHelpers.addressIsAllowedToProduceNextBlock(
        localAddress, protocolContext, parentHeader)) {
      return super.mineBlock();
    }

    return true; // terminate mining.
  }
}
