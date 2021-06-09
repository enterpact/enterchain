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
package org.enterchain.enter.consensus.ibftlegacy;

import static org.enterchain.enter.consensus.ibftlegacy.IbftBlockHeaderValidationRulesetFactory.ibftBlockHeaderValidator;

import org.enterchain.enter.config.GenesisConfigOptions;
import org.enterchain.enter.config.IbftLegacyConfigOptions;
import org.enterchain.enter.ethereum.core.PrivacyParameters;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.mainnet.MainnetBlockBodyValidator;
import org.enterchain.enter.ethereum.mainnet.MainnetBlockImporter;
import org.enterchain.enter.ethereum.mainnet.MainnetProtocolSpecs;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.mainnet.ProtocolScheduleBuilder;
import org.enterchain.enter.ethereum.mainnet.ProtocolSpecAdapters;
import org.enterchain.enter.ethereum.mainnet.ProtocolSpecBuilder;

import java.math.BigInteger;

/** Defines the protocol behaviours for a blockchain using IBFT. */
public class IbftProtocolSchedule {

  private static final BigInteger DEFAULT_CHAIN_ID = BigInteger.ONE;

  public static ProtocolSchedule create(
      final GenesisConfigOptions config,
      final PrivacyParameters privacyParameters,
      final boolean isRevertReasonEnabled) {
    final IbftLegacyConfigOptions ibftConfig = config.getIbftLegacyConfigOptions();
    final long blockPeriod = ibftConfig.getBlockPeriodSeconds();

    return new ProtocolScheduleBuilder(
            config,
            DEFAULT_CHAIN_ID,
            ProtocolSpecAdapters.create(
                0,
                builder ->
                    applyIbftChanges(
                        blockPeriod, builder, config.isQuorum(), ibftConfig.getCeil2Nby3Block())),
            privacyParameters,
            isRevertReasonEnabled,
            config.isQuorum())
        .createProtocolSchedule();
  }

  public static ProtocolSchedule create(
      final GenesisConfigOptions config, final boolean isRevertReasonEnabled) {
    return create(config, PrivacyParameters.DEFAULT, isRevertReasonEnabled);
  }

  private static ProtocolSpecBuilder applyIbftChanges(
      final long secondsBetweenBlocks,
      final ProtocolSpecBuilder builder,
      final boolean goQuorumMode,
      final long ceil2nBy3Block) {
    return builder
        .blockHeaderValidatorBuilder(ibftBlockHeaderValidator(secondsBetweenBlocks, ceil2nBy3Block))
        .ommerHeaderValidatorBuilder(ibftBlockHeaderValidator(secondsBetweenBlocks, ceil2nBy3Block))
        .blockBodyValidatorBuilder(MainnetBlockBodyValidator::new)
        .blockValidatorBuilder(MainnetProtocolSpecs.blockValidatorBuilder(goQuorumMode))
        .blockImporterBuilder(MainnetBlockImporter::new)
        .difficultyCalculator((time, parent, protocolContext) -> BigInteger.ONE)
        .blockReward(Wei.ZERO)
        .skipZeroBlockRewards(true)
        .blockHeaderFunctions(new LegacyIbftBlockHeaderFunctions());
  }
}
