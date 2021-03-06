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
package org.enterchain.enter.ethereum.vm.operations;

import org.enterchain.enter.ethereum.core.Account;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.Gas;
import org.enterchain.enter.ethereum.vm.AbstractOperation;
import org.enterchain.enter.ethereum.vm.EVM;
import org.enterchain.enter.ethereum.vm.ExceptionalHaltReason;
import org.enterchain.enter.ethereum.vm.GasCalculator;
import org.enterchain.enter.ethereum.vm.MessageFrame;
import org.enterchain.enter.ethereum.vm.Words;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;

public class ExtCodeCopyOperation extends AbstractOperation {

  public ExtCodeCopyOperation(final GasCalculator gasCalculator) {
    super(0x3C, "EXTCODECOPY", 4, 0, false, 1, gasCalculator);
  }

  @Override
  public OperationResult execute(final MessageFrame frame, final EVM evm) {
    final Address address = Words.toAddress(frame.popStackItem());
    final UInt256 memOffset = UInt256.fromBytes(frame.popStackItem());
    final UInt256 sourceOffset = UInt256.fromBytes(frame.popStackItem());
    final UInt256 numBytes = UInt256.fromBytes(frame.popStackItem());

    final boolean accountIsWarm =
        frame.warmUpAddress(address) || gasCalculator().isPrecompile(address);
    final Gas cost =
        gasCalculator()
            .extCodeCopyOperationGasCost(frame, memOffset, numBytes)
            .plus(
                accountIsWarm
                    ? gasCalculator().getWarmStorageReadCost()
                    : gasCalculator().getColdAccountAccessCost());

    final Optional<Gas> optionalCost = Optional.of(cost);
    if (frame.getRemainingGas().compareTo(cost) < 0) {
      return new OperationResult(optionalCost, Optional.of(ExceptionalHaltReason.INSUFFICIENT_GAS));
    }

    final Account account = frame.getWorldState().get(address);
    final Bytes code = account != null ? account.getCode() : Bytes.EMPTY;

    frame.writeMemory(memOffset, sourceOffset, numBytes, code);
    return new OperationResult(optionalCost, Optional.empty());
  }
}
