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
package org.enterchain.enter.ethereum.blockcreation;

import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.BlockHeaderBuilder;
import org.enterchain.enter.ethereum.core.SealableBlockHeader;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.eth.transactions.PendingTransactions;
import org.enterchain.enter.ethereum.mainnet.EthHash;
import org.enterchain.enter.ethereum.mainnet.PoWSolution;
import org.enterchain.enter.ethereum.mainnet.PoWSolver;
import org.enterchain.enter.ethereum.mainnet.PoWSolverInputs;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;

import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.apache.tuweni.units.bigints.UInt256;

public class PoWBlockCreator extends AbstractBlockCreator {

  private final PoWSolver nonceSolver;

  public PoWBlockCreator(
      final Address coinbase,
      final ExtraDataCalculator extraDataCalculator,
      final PendingTransactions pendingTransactions,
      final ProtocolContext protocolContext,
      final ProtocolSchedule protocolSchedule,
      final GasLimitCalculator gasLimitCalculator,
      final PoWSolver nonceSolver,
      final Wei minTransactionGasPrice,
      final Double minBlockOccupancyRatio,
      final BlockHeader parentHeader) {
    super(
        coinbase,
        extraDataCalculator,
        pendingTransactions,
        protocolContext,
        protocolSchedule,
        gasLimitCalculator,
        minTransactionGasPrice,
        coinbase,
        minBlockOccupancyRatio,
        parentHeader);

    this.nonceSolver = nonceSolver;
  }

  @Override
  protected BlockHeader createFinalBlockHeader(final SealableBlockHeader sealableBlockHeader) {
    final PoWSolverInputs workDefinition = generateNonceSolverInputs(sealableBlockHeader);
    final PoWSolution solution;
    try {
      solution = nonceSolver.solveFor(PoWSolver.PoWSolverJob.createFromInputs(workDefinition));
    } catch (final InterruptedException ex) {
      throw new CancellationException();
    } catch (final ExecutionException ex) {
      throw new RuntimeException("Failure occurred during nonce calculations.", ex);
    }
    return BlockHeaderBuilder.create()
        .populateFrom(sealableBlockHeader)
        .mixHash(solution.getMixHash())
        .nonce(solution.getNonce())
        .blockHeaderFunctions(blockHeaderFunctions)
        .buildBlockHeader();
  }

  private PoWSolverInputs generateNonceSolverInputs(final SealableBlockHeader sealableBlockHeader) {
    final BigInteger difficulty = sealableBlockHeader.getDifficulty().toBigInteger();
    final UInt256 target =
        difficulty.equals(BigInteger.ONE)
            ? UInt256.MAX_VALUE
            : UInt256.valueOf(EthHash.TARGET_UPPER_BOUND.divide(difficulty));

    return new PoWSolverInputs(
        target, EthHash.hashHeader(sealableBlockHeader), sealableBlockHeader.getNumber());
  }

  public Optional<PoWSolverInputs> getWorkDefinition() {
    return nonceSolver.getWorkDefinition();
  }

  public Optional<Long> getHashesPerSecond() {
    return nonceSolver.hashesPerSecond();
  }

  public boolean submitWork(final PoWSolution solution) {
    return nonceSolver.submitSolution(solution);
  }

  @Override
  public void cancel() {
    super.cancel();
    nonceSolver.cancel();
  }
}
