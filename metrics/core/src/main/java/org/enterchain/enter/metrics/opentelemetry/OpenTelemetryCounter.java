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
package org.enterchain.enter.metrics.opentelemetry;

import org.enterchain.enter.plugin.services.metrics.Counter;
import org.enterchain.enter.plugin.services.metrics.LabelledMetric;

import java.util.ArrayList;
import java.util.List;

import io.opentelemetry.api.common.Labels;
import io.opentelemetry.api.metrics.LongCounter;

public class OpenTelemetryCounter implements LabelledMetric<Counter> {

  private final LongCounter counter;
  private final String[] labelNames;

  public OpenTelemetryCounter(final LongCounter counter, final String... labelNames) {
    this.counter = counter;
    this.labelNames = labelNames;
  }

  @Override
  public Counter labels(final String... labelValues) {
    List<String> labelKeysAndValues = new ArrayList<>();
    for (int i = 0; i < labelNames.length; i++) {
      labelKeysAndValues.add(labelNames[i]);
      labelKeysAndValues.add(labelValues[i]);
    }
    final Labels labels = Labels.of(labelKeysAndValues.toArray(new String[] {}));
    LongCounter.BoundLongCounter boundLongCounter = counter.bind(labels);
    return new OpenTelemetryCounter.UnlabelledCounter(boundLongCounter);
  }

  private static class UnlabelledCounter implements Counter {
    private final LongCounter.BoundLongCounter counter;

    private UnlabelledCounter(final LongCounter.BoundLongCounter counter) {
      this.counter = counter;
    }

    @Override
    public void inc() {
      counter.add(1);
    }

    @Override
    public void inc(final long amount) {
      counter.add(amount);
    }
  }
}
