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
import org.enterchain.enter.ethereum.vm.EVM;
import org.enterchain.enter.ethereum.vm.GasCalculator;
import org.enterchain.enter.ethereum.vm.MessageFrame;
import org.enterchain.enter.ethereum.vm.Words;

public class AddressOperation extends AbstractFixedCostOperation {

  public AddressOperation(final GasCalculator gasCalculator) {
    super(0x30, "ADDRESS", 0, 1, false, 1, gasCalculator, gasCalculator.getBaseTierGasCost());
  }

  @Override
  public OperationResult executeFixedCostOperation(final MessageFrame frame, final EVM evm) {
    final Address address = frame.getRecipientAddress();
    frame.pushStackItem(Words.fromAddress(address));

    return successResponse;
  }
}
