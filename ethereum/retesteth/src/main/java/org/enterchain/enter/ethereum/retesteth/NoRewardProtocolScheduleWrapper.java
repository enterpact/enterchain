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
package org.enterchain.enter.ethereum.retesteth;

import org.enterchain.enter.ethereum.BlockValidator;
import org.enterchain.enter.ethereum.MainnetBlockValidator;
import org.enterchain.enter.ethereum.core.BlockImporter;
import org.enterchain.enter.ethereum.core.TransactionFilter;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.core.fees.TransactionGasBudgetCalculator;
import org.enterchain.enter.ethereum.mainnet.BlockProcessor;
import org.enterchain.enter.ethereum.mainnet.MainnetBlockImporter;
import org.enterchain.enter.ethereum.mainnet.MainnetBlockProcessor;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.mainnet.ProtocolSpec;
import org.enterchain.enter.ethereum.worldstate.WorldStateArchive;

import java.math.BigInteger;
import java.util.Optional;
import java.util.stream.Stream;

public class NoRewardProtocolScheduleWrapper implements ProtocolSchedule {

  private final ProtocolSchedule delegate;

  NoRewardProtocolScheduleWrapper(final ProtocolSchedule delegate) {
    this.delegate = delegate;
  }

  @Override
  public ProtocolSpec getByBlockNumber(final long number) {
    final ProtocolSpec original = delegate.getByBlockNumber(number);
    final BlockProcessor noRewardBlockProcessor =
        new MainnetBlockProcessor(
            original.getTransactionProcessor(),
            original.getTransactionReceiptFactory(),
            Wei.ZERO,
            original.getMiningBeneficiaryCalculator(),
            original.isSkipZeroBlockRewards(),
            TransactionGasBudgetCalculator.frontier(),
            Optional.empty());
    final BlockValidator noRewardBlockValidator =
        new MainnetBlockValidator(
            original.getBlockHeaderValidator(),
            original.getBlockBodyValidator(),
            noRewardBlockProcessor,
            original.getBadBlocksManager());
    final BlockImporter noRewardBlockImporter = new MainnetBlockImporter(noRewardBlockValidator);
    return new ProtocolSpec(
        original.getName(),
        original.getEvm(),
        original.getTransactionValidator(),
        original.getTransactionProcessor(),
        original.getPrivateTransactionProcessor(),
        original.getBlockHeaderValidator(),
        original.getOmmerHeaderValidator(),
        original.getBlockBodyValidator(),
        noRewardBlockProcessor,
        noRewardBlockImporter,
        noRewardBlockValidator,
        original.getBlockHeaderFunctions(),
        original.getTransactionReceiptFactory(),
        original.getDifficultyCalculator(),
        Wei.ZERO, // block reward
        original.getMiningBeneficiaryCalculator(),
        original.getPrecompileContractRegistry(),
        original.isSkipZeroBlockRewards(),
        original.getGasCalculator(),
        original.getTransactionPriceCalculator(),
        original.getEip1559(),
        original.getGasBudgetCalculator(),
        original.getBadBlocksManager(),
        Optional.empty());
  }

  @Override
  public Stream<Long> streamMilestoneBlocks() {
    return delegate.streamMilestoneBlocks();
  }

  @Override
  public Optional<BigInteger> getChainId() {
    return delegate.getChainId();
  }

  @Override
  public void setTransactionFilter(final TransactionFilter transactionFilter) {
    delegate.setTransactionFilter(transactionFilter);
  }

  @Override
  public void setPublicWorldStateArchiveForPrivacyBlockProcessor(
      final WorldStateArchive publicWorldStateArchive) {
    delegate.setPublicWorldStateArchiveForPrivacyBlockProcessor(publicWorldStateArchive);
  }
}
