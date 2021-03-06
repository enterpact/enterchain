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
package org.enterchain.enter.evmtool;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.enterchain.enter.evmtool.exception.UnsupportedForkException;

import org.junit.Test;
import picocli.CommandLine;

public class StateTestSubCommandTest {

  @Test
  public void shouldDetectUnsupportedFork() {
    final StateTestSubCommand stateTestSubCommand = new StateTestSubCommand(new EvmToolCommand());
    final CommandLine cmd = new CommandLine(stateTestSubCommand);
    cmd.parseArgs(
        StateTestSubCommandTest.class.getResource("unsupported-fork-state-test.json").getPath());
    assertThatThrownBy(stateTestSubCommand::run)
        .hasMessageContaining("Fork 'UnknownFork' not supported")
        .isInstanceOf(UnsupportedForkException.class);
  }

  @Test
  public void shouldWorkWithValidStateTest() {
    final StateTestSubCommand stateTestSubCommand = new StateTestSubCommand(new EvmToolCommand());
    final CommandLine cmd = new CommandLine(stateTestSubCommand);
    cmd.parseArgs(StateTestSubCommandTest.class.getResource("valid-state-test.json").getPath());
    stateTestSubCommand.run();
  }

  @Test
  public void shouldWorkWithValidAccessListStateTest() {
    final StateTestSubCommand stateTestSubCommand = new StateTestSubCommand(new EvmToolCommand());
    final CommandLine cmd = new CommandLine(stateTestSubCommand);
    cmd.parseArgs(StateTestSubCommandTest.class.getResource("access-list.json").getPath());
    stateTestSubCommand.run();
  }
}
