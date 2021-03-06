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

import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.Gas;
import org.enterchain.enter.ethereum.core.Log;
import org.enterchain.enter.ethereum.core.LogTopic;
import org.enterchain.enter.ethereum.vm.AbstractOperation;
import org.enterchain.enter.ethereum.vm.EVM;
import org.enterchain.enter.ethereum.vm.ExceptionalHaltReason;
import org.enterchain.enter.ethereum.vm.GasCalculator;
import org.enterchain.enter.ethereum.vm.MessageFrame;

import java.util.Optional;

import com.google.common.collect.ImmutableList;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;

public class LogOperation extends AbstractOperation {

  private final int numTopics;

  public LogOperation(final int numTopics, final GasCalculator gasCalculator) {
    super(0xA0 + numTopics, "LOG" + numTopics, numTopics + 2, 0, false, 1, gasCalculator);
    this.numTopics = numTopics;
  }

  @Override
  public OperationResult execute(final MessageFrame frame, final EVM evm) {
    final UInt256 dataLocation = UInt256.fromBytes(frame.popStackItem());
    final UInt256 numBytes = UInt256.fromBytes(frame.popStackItem());

    final Gas cost = gasCalculator().logOperationGasCost(frame, dataLocation, numBytes, numTopics);
    final Optional<Gas> optionalCost = Optional.of(cost);
    if (frame.isStatic()) {
      return new OperationResult(
          optionalCost, Optional.of(ExceptionalHaltReason.ILLEGAL_STATE_CHANGE));
    } else if (frame.getRemainingGas().compareTo(cost) < 0) {
      return new OperationResult(optionalCost, Optional.of(ExceptionalHaltReason.INSUFFICIENT_GAS));
    }

    final Address address = frame.getRecipientAddress();

    final Bytes data = frame.readMemory(dataLocation, numBytes);

    final ImmutableList.Builder<LogTopic> builder =
        ImmutableList.builderWithExpectedSize(numTopics);
    for (int i = 0; i < numTopics; i++) {
      builder.add(LogTopic.create(frame.popStackItem()));
    }

    frame.addLog(new Log(address, data, builder.build()));
    return new OperationResult(optionalCost, Optional.empty());
  }
}
