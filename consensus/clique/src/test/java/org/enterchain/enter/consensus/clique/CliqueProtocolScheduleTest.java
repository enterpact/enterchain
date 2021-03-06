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
package org.enterchain.enter.consensus.clique;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.enterchain.enter.config.CliqueConfigOptions;
import org.enterchain.enter.config.GenesisConfigFile;
import org.enterchain.enter.config.GenesisConfigOptions;
import org.enterchain.enter.crypto.NodeKey;
import org.enterchain.enter.crypto.NodeKeyUtils;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.mainnet.ProtocolSpec;

import org.junit.Test;

public class CliqueProtocolScheduleTest {

  private static final NodeKey NODE_KEY = NodeKeyUtils.generate();
  private final GenesisConfigOptions genesisConfig = mock(GenesisConfigOptions.class);

  @Test
  public void protocolSpecsAreCreatedAtBlockDefinedInJson() {
    final String jsonInput =
        "{\"config\": "
            + "{\"chainId\": 4,\n"
            + "\"homesteadBlock\": 1,\n"
            + "\"eip150Block\": 2,\n"
            + "\"eip158Block\": 3,\n"
            + "\"byzantiumBlock\": 1035301}"
            + "}";

    final GenesisConfigOptions config = GenesisConfigFile.fromConfig(jsonInput).getConfigOptions();
    final ProtocolSchedule protocolSchedule =
        CliqueProtocolSchedule.create(config, NODE_KEY, false);

    final ProtocolSpec homesteadSpec = protocolSchedule.getByBlockNumber(1);
    final ProtocolSpec tangerineWhistleSpec = protocolSchedule.getByBlockNumber(2);
    final ProtocolSpec spuriousDragonSpec = protocolSchedule.getByBlockNumber(3);
    final ProtocolSpec byzantiumSpec = protocolSchedule.getByBlockNumber(1035301);

    assertThat(homesteadSpec.equals(tangerineWhistleSpec)).isFalse();
    assertThat(tangerineWhistleSpec.equals(spuriousDragonSpec)).isFalse();
    assertThat(spuriousDragonSpec.equals(byzantiumSpec)).isFalse();
  }

  @Test
  public void parametersAlignWithMainnetWithAdjustments() {
    final ProtocolSpec homestead =
        CliqueProtocolSchedule.create(GenesisConfigFile.DEFAULT.getConfigOptions(), NODE_KEY, false)
            .getByBlockNumber(0);

    assertThat(homestead.getName()).isEqualTo("Frontier");
    assertThat(homestead.getBlockReward()).isEqualTo(Wei.ZERO);
    assertThat(homestead.isSkipZeroBlockRewards()).isEqualTo(true);
    assertThat(homestead.getDifficultyCalculator()).isInstanceOf(CliqueDifficultyCalculator.class);
  }

  @Test
  public void zeroEpochLengthThrowsException() {
    final CliqueConfigOptions cliqueOptions = mock(CliqueConfigOptions.class);
    when(cliqueOptions.getEpochLength()).thenReturn(0L);
    when(genesisConfig.getCliqueConfigOptions()).thenReturn(cliqueOptions);

    assertThatThrownBy(() -> CliqueProtocolSchedule.create(genesisConfig, NODE_KEY, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Epoch length in config must be greater than zero");
  }

  @Test
  public void negativeEpochLengthThrowsException() {
    final CliqueConfigOptions cliqueOptions = mock(CliqueConfigOptions.class);
    when(cliqueOptions.getEpochLength()).thenReturn(-3000L);
    when(genesisConfig.getCliqueConfigOptions()).thenReturn(cliqueOptions);

    assertThatThrownBy(() -> CliqueProtocolSchedule.create(genesisConfig, NODE_KEY, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Epoch length in config must be greater than zero");
  }
}
