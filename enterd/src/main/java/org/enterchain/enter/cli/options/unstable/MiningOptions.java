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
package org.enterchain.enter.cli.options.unstable;

import static org.enterchain.enter.ethereum.core.MiningParameters.DEFAULT_REMOTE_SEALERS_LIMIT;
import static org.enterchain.enter.ethereum.core.MiningParameters.DEFAULT_REMOTE_SEALERS_TTL;

import picocli.CommandLine;

public class MiningOptions {

  @CommandLine.Option(
      hidden = true,
      names = {"--Xminer-remote-sealers-limit"},
      description =
          "Limits the number of remote sealers that can submit their hashrates (default: ${DEFAULT-VALUE})")
  private final Integer remoteSealersLimit = DEFAULT_REMOTE_SEALERS_LIMIT;

  @CommandLine.Option(
      hidden = true,
      names = {"--Xminer-remote-sealers-hashrate-ttl"},
      description =
          "Specifies the lifetime of each entry in the cache. An entry will be automatically deleted if no update has been received before the deadline (default: ${DEFAULT-VALUE} minutes)")
  private final Long remoteSealersTimeToLive = DEFAULT_REMOTE_SEALERS_TTL;

  @SuppressWarnings({"FieldCanBeFinal", "FieldMayBeFinal"}) // PicoCLI requires non-final Strings.
  @CommandLine.Option(
      hidden = true,
      names = {"--Xminer-stratum-extranonce"},
      description = "Extranonce for Stratum network miners (default: ${DEFAULT-VALUE})")
  private String stratumExtranonce = "080c";

  public static MiningOptions create() {
    return new MiningOptions();
  }

  public Integer getRemoteSealersLimit() {
    return remoteSealersLimit;
  }

  public Long getRemoteSealersTimeToLive() {
    return remoteSealersTimeToLive;
  }

  public String getStratumExtranonce() {
    return stratumExtranonce;
  }
}
