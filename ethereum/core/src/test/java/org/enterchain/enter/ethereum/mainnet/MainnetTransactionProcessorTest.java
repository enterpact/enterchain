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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.core.Account;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.ProcessableBlockHeader;
import org.enterchain.enter.ethereum.core.Transaction;
import org.enterchain.enter.ethereum.core.WorldUpdater;
import org.enterchain.enter.ethereum.core.fees.CoinbaseFeePriceCalculator;
import org.enterchain.enter.ethereum.core.fees.TransactionPriceCalculator;
import org.enterchain.enter.ethereum.transaction.TransactionInvalidReason;
import org.enterchain.enter.ethereum.vm.BlockHashLookup;
import org.enterchain.enter.ethereum.vm.GasCalculator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MainnetTransactionProcessorTest {

  private static final int MAX_STACK_SIZE = 1024;

  private MainnetTransactionProcessor transactionProcessor;

  @Mock private GasCalculator gasCalculator;
  @Mock private MainnetTransactionValidator transactionValidator;
  @Mock private AbstractMessageProcessor contractCreationProcessor;
  @Mock private AbstractMessageProcessor messageCallProcessor;

  @Mock private Blockchain blockchain;
  @Mock private WorldUpdater worldState;
  @Mock private ProcessableBlockHeader blockHeader;
  @Mock private Transaction transaction;
  @Mock private BlockHashLookup blockHashLookup;

  @Before
  public void before() {
    transactionProcessor =
        new MainnetTransactionProcessor(
            gasCalculator,
            transactionValidator,
            contractCreationProcessor,
            messageCallProcessor,
            false,
            MAX_STACK_SIZE,
            Account.DEFAULT_VERSION,
            TransactionPriceCalculator.frontier(),
            CoinbaseFeePriceCalculator.frontier());
  }

  @Test
  public void shouldCallTransactionValidatorWithExpectedTransactionValidationParams() {
    final ArgumentCaptor<TransactionValidationParams> txValidationParamCaptor =
        transactionValidationParamCaptor();

    final TransactionValidationParams expectedValidationParams =
        ImmutableTransactionValidationParams.builder().build();

    transactionProcessor.processTransaction(
        blockchain,
        worldState,
        blockHeader,
        transaction,
        Address.fromHexString("1"),
        blockHashLookup,
        false,
        ImmutableTransactionValidationParams.builder().build());

    assertThat(txValidationParamCaptor.getValue())
        .isEqualToComparingFieldByField(expectedValidationParams);
  }

  private ArgumentCaptor<TransactionValidationParams> transactionValidationParamCaptor() {
    final ArgumentCaptor<TransactionValidationParams> txValidationParamCaptor =
        ArgumentCaptor.forClass(TransactionValidationParams.class);
    when(transactionValidator.validate(any(), any())).thenReturn(ValidationResult.valid());
    // returning invalid transaction to halt method execution
    when(transactionValidator.validateForSender(any(), any(), txValidationParamCaptor.capture()))
        .thenReturn(ValidationResult.invalid(TransactionInvalidReason.INCORRECT_NONCE));
    return txValidationParamCaptor;
  }
}
