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
package org.enterchain.enter.consensus.common.bft.blockcreation;

import org.enterchain.enter.consensus.common.bft.BftBlockHeaderFunctions;
import org.enterchain.enter.consensus.common.bft.BftExtraDataCodec;
import org.enterchain.enter.consensus.common.bft.BftHelpers;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.blockcreation.AbstractBlockCreator;
import org.enterchain.enter.ethereum.blockcreation.GasLimitCalculator;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.BlockHeaderBuilder;
import org.enterchain.enter.ethereum.core.SealableBlockHeader;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.eth.transactions.PendingTransactions;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;

// This class is responsible for creating a block without committer seals (basically it was just
// too hard to coordinate with the state machine).
public class BftBlockCreator extends AbstractBlockCreator {

  private final BftExtraDataCodec bftExtraDataCodec;

  public BftBlockCreator(
      final Address localAddress,
      final ExtraDataCalculator extraDataCalculator,
      final PendingTransactions pendingTransactions,
      final ProtocolContext protocolContext,
      final ProtocolSchedule protocolSchedule,
      final GasLimitCalculator gasLimitCalculator,
      final Wei minTransactionGasPrice,
      final Double minBlockOccupancyRatio,
      final BlockHeader parentHeader,
      final Address miningBeneficiary,
      final BftExtraDataCodec bftExtraDataCodec) {
    super(
        localAddress,
        extraDataCalculator,
        pendingTransactions,
        protocolContext,
        protocolSchedule,
        gasLimitCalculator,
        minTransactionGasPrice,
        miningBeneficiary,
        minBlockOccupancyRatio,
        parentHeader);
    this.bftExtraDataCodec = bftExtraDataCodec;
  }

  @Override
  protected BlockHeader createFinalBlockHeader(final SealableBlockHeader sealableBlockHeader) {
    final BlockHeaderBuilder builder =
        BlockHeaderBuilder.create()
            .populateFrom(sealableBlockHeader)
            .mixHash(BftHelpers.EXPECTED_MIX_HASH)
            .nonce(0L)
            .blockHeaderFunctions(BftBlockHeaderFunctions.forCommittedSeal(bftExtraDataCodec));

    return builder.buildBlockHeader();
  }
}
