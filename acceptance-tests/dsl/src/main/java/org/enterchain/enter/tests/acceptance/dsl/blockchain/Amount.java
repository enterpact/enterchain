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
package org.enterchain.enter.tests.acceptance.dsl.blockchain;

import static org.web3j.utils.Convert.Unit.ETHER;
import static org.web3j.utils.Convert.Unit.WEI;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.web3j.utils.Convert;
import org.web3j.utils.Convert.Unit;

public class Amount {

  private final BigDecimal value;
  private final Unit unit;

  private Amount(final BigDecimal value, final Unit unit) {
    this.value = value;
    this.unit = unit;
  }

  public static Amount ether(final long value) {
    return new Amount(BigDecimal.valueOf(value), ETHER);
  }

  public static Amount wei(final BigInteger value) {
    return new Amount(new BigDecimal(value), WEI);
  }

  public BigDecimal getValue() {
    return value;
  }

  public Unit getUnit() {
    return unit;
  }

  public Amount subtract(final Amount subtracting) {

    final Unit denominator;
    if (unit.getWeiFactor().compareTo(subtracting.unit.getWeiFactor()) < 0) {
      denominator = unit;
    } else {
      denominator = subtracting.unit;
    }

    final BigDecimal result =
        Convert.fromWei(
            Convert.toWei(value, unit).subtract(Convert.toWei(subtracting.value, subtracting.unit)),
            denominator);

    return new Amount(result, denominator);
  }
}
