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
package org.enterchain.enter.ethereum.chain;

import org.enterchain.enter.ethereum.mainnet.PoWSolution;
import org.enterchain.enter.ethereum.mainnet.PoWSolverInputs;

import java.util.function.Function;

/** Observer of new work for the PoW algorithm */
public interface PoWObserver {

  /**
   * Send a new proof-of-work job to observers
   *
   * @param jobInput the proof-of-work job
   */
  void newJob(PoWSolverInputs jobInput);

  /**
   * Sets a callback for the observer to provide solutions to jobs.
   *
   * @param submitSolutionCallback the callback to set on the observer, consuming a solution and
   *     returning true if the solution is accepted, false if rejected.
   */
  void setSubmitWorkCallback(Function<PoWSolution, Boolean> submitSolutionCallback);
}
