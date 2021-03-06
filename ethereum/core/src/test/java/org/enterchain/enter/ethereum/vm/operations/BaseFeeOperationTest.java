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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.Gas;
import org.enterchain.enter.ethereum.mainnet.BerlinGasCalculator;
import org.enterchain.enter.ethereum.vm.ExceptionalHaltReason;
import org.enterchain.enter.ethereum.vm.GasCalculator;
import org.enterchain.enter.ethereum.vm.MessageFrame;
import org.enterchain.enter.ethereum.vm.Operation;
import org.enterchain.enter.ethereum.vm.Operation.OperationResult;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.Test;

public class BaseFeeOperationTest {
  private final GasCalculator gasCalculator = new BerlinGasCalculator();

  @Test
  public void shouldReturnGasCost() {
    final MessageFrame frame = createMessageFrame(100, Optional.of(5L));
    final Operation operation = new BaseFeeOperation(gasCalculator);
    final OperationResult result = operation.execute(frame, null);
    assertThat(result.getGasCost()).contains(gasCalculator.getBaseTierGasCost());
    assertSuccessResult(result);
  }

  @Test
  public void shouldWriteBaseFeeToStack() {
    final MessageFrame frame = createMessageFrame(100, Optional.of(5L));
    final Operation operation = new BaseFeeOperation(gasCalculator);
    final OperationResult result = operation.execute(frame, null);
    verify(frame).pushStackItem(eq(Bytes32.leftPad(Bytes.ofUnsignedLong(5L))));
    assertSuccessResult(result);
  }

  @Test
  public void shouldHaltIfNoBaseFeeInBlockHeader() {
    final MessageFrame frame = createMessageFrame(100, Optional.empty());
    final Operation operation = new BaseFeeOperation(gasCalculator);
    final OperationResult result = operation.execute(frame, null);
    assertExceptionalHalt(result, ExceptionalHaltReason.INVALID_OPERATION);
  }

  private void assertSuccessResult(final OperationResult result) {
    assertThat(result).isNotNull();
    assertThat(result.getHaltReason()).isEmpty();
  }

  private void assertExceptionalHalt(
      final OperationResult result, final ExceptionalHaltReason reason) {
    assertThat(result).isNotNull();
    assertThat(result.getHaltReason()).contains(reason);
  }

  private MessageFrame createMessageFrame(final long initialGas, final Optional<Long> baseFee) {
    return createMessageFrame(Gas.of(initialGas), baseFee);
  }

  private MessageFrame createMessageFrame(final Gas initialGas, final Optional<Long> baseFee) {
    final MessageFrame frame = mock(MessageFrame.class);
    when(frame.getRemainingGas()).thenReturn(initialGas);
    final BlockHeader blockHeader = mock(BlockHeader.class);
    when(blockHeader.getBaseFee()).thenReturn(baseFee);
    when(frame.getBlockHeader()).thenReturn(blockHeader);
    return frame;
  }
}
