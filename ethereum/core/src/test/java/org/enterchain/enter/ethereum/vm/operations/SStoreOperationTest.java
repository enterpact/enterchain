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
import static org.enterchain.enter.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryWorldStateArchive;
import static org.enterchain.enter.ethereum.vm.ExceptionalHaltReason.INSUFFICIENT_GAS;
import static org.mockito.Mockito.mock;

import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.BlockHeaderTestFixture;
import org.enterchain.enter.ethereum.core.Gas;
import org.enterchain.enter.ethereum.core.MessageFrameTestFixture;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.core.WorldUpdater;
import org.enterchain.enter.ethereum.mainnet.ConstantinopleGasCalculator;
import org.enterchain.enter.ethereum.vm.ExceptionalHaltReason;
import org.enterchain.enter.ethereum.vm.GasCalculator;
import org.enterchain.enter.ethereum.vm.MessageFrame;
import org.enterchain.enter.ethereum.vm.Operation.OperationResult;
import org.enterchain.enter.ethereum.worldstate.WorldStateArchive;

import java.util.Arrays;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes32;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class SStoreOperationTest {

  private final Gas minimumGasAvailable;
  private final Gas initialGas;
  private final Gas remainingGas;
  private final Optional<ExceptionalHaltReason> expectedHalt;

  private static final GasCalculator gasCalculator = new ConstantinopleGasCalculator();

  private static final Object[][] testData = {
    {
      SStoreOperation.FRONTIER_MINIMUM, Gas.of(200), Gas.of(200), null,
    },
    {
      SStoreOperation.EIP_1706_MINIMUM, Gas.of(200), Gas.of(200), INSUFFICIENT_GAS,
    },
    {
      SStoreOperation.FRONTIER_MINIMUM, Gas.of(10_000), Gas.of(10_000), null,
    },
    {
      SStoreOperation.EIP_1706_MINIMUM, Gas.of(10_000), Gas.of(10_000), null,
    },
    {
      SStoreOperation.FRONTIER_MINIMUM, Gas.of(10_000), Gas.of(200), null,
    },
    {
      SStoreOperation.EIP_1706_MINIMUM, Gas.of(10_000), Gas.of(200), INSUFFICIENT_GAS,
    },
  };

  public SStoreOperationTest(
      final Gas minimumGasAvailable,
      final Gas initialGas,
      final Gas remainingGas,
      final ExceptionalHaltReason expectedHalt) {
    this.minimumGasAvailable = minimumGasAvailable;
    this.initialGas = initialGas;
    this.remainingGas = remainingGas;
    this.expectedHalt = Optional.ofNullable(expectedHalt);
  }

  @Parameterized.Parameters(
      name = "{index}: minimum gas {0}, initial gas {1}, remaining gas {2}, expected halt {3}")
  public static Iterable<Object[]> data() {
    return Arrays.asList(testData);
  }

  private MessageFrame createMessageFrame(
      final Address address, final Gas initialGas, final Gas remainingGas) {
    final Blockchain blockchain = mock(Blockchain.class);

    final WorldStateArchive worldStateArchive = createInMemoryWorldStateArchive();
    final WorldUpdater worldStateUpdater = worldStateArchive.getMutable().updater();
    final BlockHeader blockHeader = new BlockHeaderTestFixture().buildHeader();
    final MessageFrame frame =
        new MessageFrameTestFixture()
            .address(address)
            .worldState(worldStateUpdater)
            .blockHeader(blockHeader)
            .blockchain(blockchain)
            .initialGas(initialGas)
            .build();
    worldStateUpdater.getOrCreate(address).getMutable().setBalance(Wei.of(1));
    worldStateUpdater.commit();
    frame.setGasRemaining(remainingGas);

    return frame;
  }

  @Test
  public void storeOperation() {
    final SStoreOperation operation = new SStoreOperation(gasCalculator, minimumGasAvailable);
    final MessageFrame frame =
        createMessageFrame(Address.fromHexString("0x18675309"), initialGas, remainingGas);
    frame.pushStackItem(Bytes32.ZERO);
    frame.pushStackItem(Bytes32.fromHexString("0x01"));

    final OperationResult result = operation.execute(frame, null);
    assertThat(result.getHaltReason()).isEqualTo(expectedHalt);
  }
}
