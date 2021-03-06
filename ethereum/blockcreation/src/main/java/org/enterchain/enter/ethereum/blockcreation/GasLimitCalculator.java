/*
 * Copyright 2019 ConsenSys AG.
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

public interface GasLimitCalculator {

  long nextGasLimit(long currentGasLimit);

  default void changeTargetGasLimit(final Long __) {
    throw new UnsupportedOperationException(
        "Can only change target gas limit on a Targeting Gas Limit Calculator");
  }

  static GasLimitCalculator constant() {
    return currentGasLimit -> currentGasLimit;
  }
}
