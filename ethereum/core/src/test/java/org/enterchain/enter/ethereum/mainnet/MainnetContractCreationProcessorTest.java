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
package org.enterchain.enter.ethereum.mainnet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.enterchain.enter.ethereum.vm.MessageFrame.State.COMPLETED_SUCCESS;
import static org.enterchain.enter.ethereum.vm.MessageFrame.State.EXCEPTIONAL_HALT;
import static org.mockito.Mockito.when;

import org.enterchain.enter.ethereum.core.Gas;
import org.enterchain.enter.ethereum.core.MessageFrameTestFixture;
import org.enterchain.enter.ethereum.mainnet.contractvalidation.PrefixCodeRule;
import org.enterchain.enter.ethereum.vm.EVM;
import org.enterchain.enter.ethereum.vm.GasCalculator;
import org.enterchain.enter.ethereum.vm.MessageFrame;
import org.enterchain.enter.ethereum.vm.OperationTracer;

import java.util.Collections;

import org.apache.tuweni.bytes.Bytes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MainnetContractCreationProcessorTest {

  @Mock GasCalculator gasCalculator;
  @Mock EVM evm;

  private MainnetContractCreationProcessor processor;

  @Test
  public void shouldThrowAnExceptionWhenCodeContractFormatInvalid() {
    processor =
        new MainnetContractCreationProcessor(
            gasCalculator,
            evm,
            true,
            Collections.singletonList(PrefixCodeRule.of()),
            1,
            Collections.emptyList());
    final Bytes contractCode = Bytes.fromHexString("EF01010101010101");
    MessageFrame messageFrame = new MessageFrameTestFixture().build();
    messageFrame.setOutputData(contractCode);
    messageFrame.setGasRemaining(Gas.of(100));

    when(gasCalculator.codeDepositGasCost(contractCode.size())).thenReturn(Gas.of(10));
    processor.codeSuccess(messageFrame, OperationTracer.NO_TRACING);
    assertThat(messageFrame.getState()).isEqualTo(EXCEPTIONAL_HALT);
  }

  @Test
  public void shouldNotThrowAnExceptionWhenCodeContractIsValid() {
    processor =
        new MainnetContractCreationProcessor(
            gasCalculator,
            evm,
            true,
            Collections.singletonList(PrefixCodeRule.of()),
            1,
            Collections.emptyList());
    final Bytes contractCode = Bytes.fromHexString("0101010101010101");
    MessageFrame messageFrame = new MessageFrameTestFixture().build();
    messageFrame.setOutputData(contractCode);
    messageFrame.setGasRemaining(Gas.of(100));

    when(gasCalculator.codeDepositGasCost(contractCode.size())).thenReturn(Gas.of(10));
    processor.codeSuccess(messageFrame, OperationTracer.NO_TRACING);
    assertThat(messageFrame.getState()).isEqualTo(COMPLETED_SUCCESS);
  }

  @Test
  public void shouldNotThrowAnExceptionWhenPrefixCodeRuleNotAdded() {
    processor =
        new MainnetContractCreationProcessor(
            gasCalculator, evm, true, Collections.emptyList(), 1, Collections.emptyList());
    final Bytes contractCode = Bytes.fromHexString("0F01010101010101");
    MessageFrame messageFrame = new MessageFrameTestFixture().build();
    messageFrame.setOutputData(contractCode);
    messageFrame.setGasRemaining(Gas.of(100));

    when(gasCalculator.codeDepositGasCost(contractCode.size())).thenReturn(Gas.of(10));
    processor.codeSuccess(messageFrame, OperationTracer.NO_TRACING);
    assertThat(messageFrame.getState()).isEqualTo(COMPLETED_SUCCESS);
  }
}
