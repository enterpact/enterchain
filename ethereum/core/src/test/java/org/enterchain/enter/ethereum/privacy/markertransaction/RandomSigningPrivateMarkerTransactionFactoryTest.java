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
package org.enterchain.enter.ethereum.privacy.markertransaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.Transaction;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.privacy.PrivateTransaction;

import java.util.Base64;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.junit.Before;
import org.junit.Test;

public class RandomSigningPrivateMarkerTransactionFactoryTest {

  private final PrivateTransaction privTransaction = mock(PrivateTransaction.class);

  private final Wei gasPrice = Wei.of(100);
  private final long gasLimit = 500;
  private final Wei value = Wei.ZERO;
  private final String enclaveKey = "enclaveKey";

  @Before
  public void setup() {
    when(privTransaction.getGasPrice()).thenReturn(gasPrice);
    when(privTransaction.getGasLimit()).thenReturn(gasLimit);
    when(privTransaction.getValue()).thenReturn(value);
  }

  @Test
  public void producedTransactionHasZeroNonceAndDifferentSendThanPrior() {
    final Address precompiledAddress = Address.fromHexString("1");

    final RandomSigningPrivateMarkerTransactionFactory factory =
        new RandomSigningPrivateMarkerTransactionFactory(precompiledAddress);

    final Transaction transaction = factory.create(enclaveKey, privTransaction);

    assertThat(transaction.getNonce()).isEqualTo(0);
    assertThat(transaction.getGasLimit()).isEqualTo(privTransaction.getGasLimit());
    assertThat(transaction.getGasPrice()).isEqualTo(privTransaction.getGasPrice());
    assertThat(transaction.getValue()).isEqualTo(privTransaction.getValue());
    assertThat(transaction.getTo()).isEqualTo(Optional.of(precompiledAddress));
    assertThat(transaction.getPayload())
        .isEqualTo(Bytes.wrap(Base64.getDecoder().decode(enclaveKey)));

    final Transaction nextTransaction = factory.create("enclaveKey", privTransaction);
    assertThat(nextTransaction.getSender()).isNotEqualTo(transaction.getSender());
  }
}
