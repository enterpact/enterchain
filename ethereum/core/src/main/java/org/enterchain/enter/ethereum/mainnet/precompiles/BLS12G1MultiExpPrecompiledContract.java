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
 *
 */
package org.enterchain.enter.ethereum.mainnet.precompiles;

import org.enterchain.enter.ethereum.core.Gas;

import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.nativelib.bls12_381.LibEthPairings;

public class BLS12G1MultiExpPrecompiledContract extends AbstractBLS12PrecompiledContract {

  public BLS12G1MultiExpPrecompiledContract() {
    super("BLS12_G1MULTIEXP", LibEthPairings.BLS12_G1MULTIEXP_OPERATION_RAW_VALUE);
  }

  @Override
  public Gas gasRequirement(final Bytes input) {
    final int k = input.size() / 160;
    return Gas.of(12L * k * getDiscount(k));
  }
}