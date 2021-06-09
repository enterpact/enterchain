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
package org.enterchain.enter.testutil;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalUnit;

public class TestClock extends Clock {
  public static Clock fixed() {
    return Clock.fixed(Instant.ofEpochSecond(10_000_000), ZoneId.systemDefault());
  }

  private Instant now = Instant.ofEpochSecond(24982948294L);

  @Override
  public ZoneId getZone() {
    return ZoneOffset.UTC;
  }

  @Override
  public Clock withZone(final ZoneId zone) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Instant instant() {
    return now;
  }

  public void stepMillis(final long millis) {
    now = now.plusMillis(millis);
  }

  public void step(final long a, final TemporalUnit unit) {
    now = now.plus(a, unit);
  }
}