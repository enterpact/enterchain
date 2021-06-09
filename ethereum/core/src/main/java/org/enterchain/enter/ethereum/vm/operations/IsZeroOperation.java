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

import org.enterchain.enter.ethereum.vm.EVM;
import org.enterchain.enter.ethereum.vm.GasCalculator;
import org.enterchain.enter.ethereum.vm.MessageFrame;

import org.apache.tuweni.units.bigints.UInt256;

public class IsZeroOperation extends AbstractFixedCostOperation {

  public IsZeroOperation(final GasCalculator gasCalculator) {
    super(0x15, "ISZERO", 1, 1, false, 1, gasCalculator, gasCalculator.getVeryLowTierGasCost());
  }

  @Override
  public OperationResult executeFixedCostOperation(final MessageFrame frame, final EVM evm) {
    final UInt256 value = UInt256.fromBytes(frame.popStackItem());

    frame.pushStackItem(value.isZero() ? UInt256.ONE.toBytes() : UInt256.ZERO.toBytes());

    return successResponse;
  }
}