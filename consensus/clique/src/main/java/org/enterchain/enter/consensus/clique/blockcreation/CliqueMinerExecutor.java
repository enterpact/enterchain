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

import org.enterchain.enter.consensus.clique.CliqueContext;
import org.enterchain.enter.consensus.clique.CliqueExtraData;
import org.enterchain.enter.consensus.common.ConsensusHelpers;
import org.enterchain.enter.consensus.common.EpochManager;
import org.enterchain.enter.consensus.common.VoteTally;
import org.enterchain.enter.crypto.NodeKey;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.blockcreation.AbstractBlockScheduler;
import org.enterchain.enter.ethereum.blockcreation.AbstractMinerExecutor;
import org.enterchain.enter.ethereum.blockcreation.GasLimitCalculator;
import org.enterchain.enter.ethereum.chain.MinedBlockObserver;
import org.enterchain.enter.ethereum.chain.PoWObserver;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.MiningParameters;
import org.enterchain.enter.ethereum.core.Util;
import org.enterchain.enter.ethereum.eth.transactions.PendingTransactions;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.util.Subscribers;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.tuweni.bytes.Bytes;

public class CliqueMinerExecutor extends AbstractMinerExecutor<CliqueBlockMiner> {

  private final Address localAddress;
  private final NodeKey nodeKey;
  private final EpochManager epochManager;

  public CliqueMinerExecutor(
      final ProtocolContext protocolContext,
      final ProtocolSchedule protocolSchedule,
      final PendingTransactions pendingTransactions,
      final NodeKey nodeKey,
      final MiningParameters miningParams,
      final AbstractBlockScheduler blockScheduler,
      final EpochManager epochManager,
      final GasLimitCalculator gasLimitCalculator) {
    super(
        protocolContext,
        protocolSchedule,
        pendingTransactions,
        miningParams,
        blockScheduler,
        gasLimitCalculator);
    this.nodeKey = nodeKey;
    this.localAddress = Util.publicKeyToAddress(nodeKey.getPublicKey());
    this.epochManager = epochManager;
  }

  @Override
  public CliqueBlockMiner createMiner(
      final Subscribers<MinedBlockObserver> observers,
      final Subscribers<PoWObserver> ethHashObservers,
      final BlockHeader parentHeader) {
    final Function<BlockHeader, CliqueBlockCreator> blockCreator =
        (header) ->
            new CliqueBlockCreator(
                localAddress, // TOOD(tmm): This can be removed (used for voting not coinbase).
                this::calculateExtraData,
                pendingTransactions,
                protocolContext,
                protocolSchedule,
                gasLimitCalculator,
                nodeKey,
                minTransactionGasPrice,
                minBlockOccupancyRatio,
                header,
                epochManager);

    return new CliqueBlockMiner(
        blockCreator,
        protocolSchedule,
        protocolContext,
        observers,
        blockScheduler,
        parentHeader,
        localAddress);
  }

  @Override
  public Optional<Address> getCoinbase() {
    return Optional.of(localAddress);
  }

  @VisibleForTesting
  Bytes calculateExtraData(final BlockHeader parentHeader) {
    final List<Address> validators = Lists.newArrayList();

    final Bytes vanityDataToInsert =
        ConsensusHelpers.zeroLeftPad(extraData, CliqueExtraData.EXTRA_VANITY_LENGTH);
    // Building ON TOP of canonical head, if the next block is epoch, include validators.
    if (epochManager.isEpochBlock(parentHeader.getNumber() + 1)) {
      final VoteTally voteTally =
          protocolContext
              .getConsensusState(CliqueContext.class)
              .getVoteTallyCache()
              .getVoteTallyAfterBlock(parentHeader);
      validators.addAll(voteTally.getValidators());
    }

    return CliqueExtraData.encodeUnsealed(vanityDataToInsert, validators);
  }
}
