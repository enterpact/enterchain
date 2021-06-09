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

import org.enterchain.enter.consensus.common.ConsensusHelpers;
import org.enterchain.enter.consensus.common.ValidatorVote;
import org.enterchain.enter.consensus.common.VoteTally;
import org.enterchain.enter.consensus.common.bft.BftContext;
import org.enterchain.enter.consensus.common.bft.BftExtraData;
import org.enterchain.enter.consensus.common.bft.BftExtraDataCodec;
import org.enterchain.enter.consensus.common.bft.Vote;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.blockcreation.GasLimitCalculator;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.MiningParameters;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.eth.transactions.PendingTransactions;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;

public class BftBlockCreatorFactory {

  private final GasLimitCalculator gasLimitCalculator;
  private final PendingTransactions pendingTransactions;
  protected final ProtocolContext protocolContext;
  protected final ProtocolSchedule protocolSchedule;
  private final BftExtraDataCodec bftExtraDataCodec;
  private final Address localAddress;
  final Address miningBeneficiary;

  private volatile Bytes vanityData;
  private volatile Wei minTransactionGasPrice;
  private volatile Double minBlockOccupancyRatio;

  public BftBlockCreatorFactory(
      final GasLimitCalculator gasLimitCalculator,
      final PendingTransactions pendingTransactions,
      final ProtocolContext protocolContext,
      final ProtocolSchedule protocolSchedule,
      final MiningParameters miningParams,
      final Address localAddress,
      final Address miningBeneficiary,
      final BftExtraDataCodec bftExtraDataCodec) {
    this.gasLimitCalculator = gasLimitCalculator;
    this.pendingTransactions = pendingTransactions;
    this.protocolContext = protocolContext;
    this.protocolSchedule = protocolSchedule;
    this.localAddress = localAddress;
    this.minTransactionGasPrice = miningParams.getMinTransactionGasPrice();
    this.minBlockOccupancyRatio = miningParams.getMinBlockOccupancyRatio();
    this.vanityData = miningParams.getExtraData();
    this.miningBeneficiary = miningBeneficiary;
    this.bftExtraDataCodec = bftExtraDataCodec;
  }

  public BftBlockCreator create(final BlockHeader parentHeader, final int round) {
    return new BftBlockCreator(
        localAddress,
        ph -> createExtraData(round, ph),
        pendingTransactions,
        protocolContext,
        protocolSchedule,
        gasLimitCalculator,
        minTransactionGasPrice,
        minBlockOccupancyRatio,
        parentHeader,
        miningBeneficiary,
        bftExtraDataCodec);
  }

  public void setExtraData(final Bytes extraData) {
    this.vanityData = extraData.copy();
  }

  public void setMinTransactionGasPrice(final Wei minTransactionGasPrice) {
    this.minTransactionGasPrice = minTransactionGasPrice;
  }

  public Wei getMinTransactionGasPrice() {
    return minTransactionGasPrice;
  }

  public Bytes createExtraData(final int round, final BlockHeader parentHeader) {
    final BftContext bftContext = protocolContext.getConsensusState(BftContext.class);
    final VoteTally voteTally = bftContext.getVoteTallyCache().getVoteTallyAfterBlock(parentHeader);

    final Optional<ValidatorVote> proposal =
        bftContext.getVoteProposer().getVote(localAddress, voteTally);

    final List<Address> validators = new ArrayList<>(voteTally.getValidators());

    final BftExtraData extraData =
        new BftExtraData(
            ConsensusHelpers.zeroLeftPad(vanityData, BftExtraDataCodec.EXTRA_VANITY_LENGTH),
            Collections.emptyList(),
            toVote(proposal),
            round,
            validators);

    return bftExtraDataCodec.encode(extraData);
  }

  public void changeTargetGasLimit(final Long targetGasLimit) {
    gasLimitCalculator.changeTargetGasLimit(targetGasLimit);
  }

  public Address getLocalAddress() {
    return localAddress;
  }

  private static Optional<Vote> toVote(final Optional<ValidatorVote> input) {
    return input
        .map(v -> Optional.of(new Vote(v.getRecipient(), v.getVotePolarity())))
        .orElse(Optional.empty());
  }
}
