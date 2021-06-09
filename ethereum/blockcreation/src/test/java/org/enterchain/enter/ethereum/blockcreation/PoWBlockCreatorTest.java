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
package org.enterchain.enter.ethereum.blockcreation;

import static org.assertj.core.api.Assertions.assertThat;

import org.enterchain.enter.config.GenesisConfigFile;
import org.enterchain.enter.config.GenesisConfigOptions;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.BlockHeaderBuilder;
import org.enterchain.enter.ethereum.core.Difficulty;
import org.enterchain.enter.ethereum.core.ExecutionContextTestFixture;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.core.MutableWorldState;
import org.enterchain.enter.ethereum.core.PrivacyParameters;
import org.enterchain.enter.ethereum.core.ProcessableBlockHeader;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.eth.transactions.PendingTransactions;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.enterchain.enter.ethereum.mainnet.EpochCalculator;
import org.enterchain.enter.ethereum.mainnet.PoWHasher;
import org.enterchain.enter.ethereum.mainnet.PoWSolver;
import org.enterchain.enter.ethereum.mainnet.ProtocolScheduleBuilder;
import org.enterchain.enter.ethereum.mainnet.ProtocolSpecAdapters;
import org.enterchain.enter.ethereum.mainnet.ValidationTestUtils;
import org.enterchain.enter.metrics.noop.NoOpMetricsSystem;
import org.enterchain.enter.plugin.services.MetricsSystem;
import org.enterchain.enter.testutil.TestClock;
import org.enterchain.enter.util.Subscribers;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.function.Function;

import com.google.common.collect.Lists;
import org.apache.tuweni.bytes.Bytes;
import org.junit.Test;

public class PoWBlockCreatorTest {

  private final Address BLOCK_1_COINBASE =
      Address.fromHexString("0x05a56e2d52c817161883f50c441c3228cfe54d9f");

  private static final long BLOCK_1_TIMESTAMP = Long.parseUnsignedLong("55ba4224", 16);

  private static final long BLOCK_1_NONCE = Long.parseLong("539bd4979fef1ec4", 16);

  private static final Bytes BLOCK_1_EXTRA_DATA =
      Bytes.fromHexString("0x476574682f76312e302e302f6c696e75782f676f312e342e32");
  private final MetricsSystem metricsSystem = new NoOpMetricsSystem();

  @Test
  public void createMainnetBlock1() throws IOException {
    final GenesisConfigOptions genesisConfigOptions = GenesisConfigFile.DEFAULT.getConfigOptions();
    final ExecutionContextTestFixture executionContextTestFixture =
        ExecutionContextTestFixture.builder()
            .protocolSchedule(
                new ProtocolScheduleBuilder(
                        genesisConfigOptions,
                        BigInteger.valueOf(42),
                        ProtocolSpecAdapters.create(0, Function.identity()),
                        PrivacyParameters.DEFAULT,
                        false,
                        genesisConfigOptions.isQuorum())
                    .createProtocolSchedule())
            .build();

    final PoWSolver solver =
        new PoWSolver(
            Lists.newArrayList(BLOCK_1_NONCE),
            PoWHasher.ETHASH_LIGHT,
            false,
            Subscribers.none(),
            new EpochCalculator.DefaultEpochCalculator());

    final PendingTransactions pendingTransactions =
        new PendingTransactions(
            TransactionPoolConfiguration.DEFAULT_TX_RETENTION_HOURS,
            1,
            5,
            TestClock.fixed(),
            metricsSystem,
            executionContextTestFixture.getProtocolContext().getBlockchain()::getChainHeadHeader,
            TransactionPoolConfiguration.DEFAULT_PRICE_BUMP);

    final PoWBlockCreator blockCreator =
        new PoWBlockCreator(
            BLOCK_1_COINBASE,
            parent -> BLOCK_1_EXTRA_DATA,
            pendingTransactions,
            executionContextTestFixture.getProtocolContext(),
            executionContextTestFixture.getProtocolSchedule(),
            gasLimit -> gasLimit,
            solver,
            Wei.ZERO,
            0.8,
            executionContextTestFixture.getBlockchain().getChainHeadHeader());

    // A Hashrate should not exist in the block creator prior to creating a block
    assertThat(blockCreator.getHashesPerSecond().isPresent()).isFalse();

    final Block actualBlock = blockCreator.createBlock(BLOCK_1_TIMESTAMP);
    final Block expectedBlock = ValidationTestUtils.readBlock(1);

    assertThat(actualBlock).isEqualTo(expectedBlock);
    assertThat(blockCreator.getHashesPerSecond().isPresent()).isTrue();
  }

  @Test
  public void createMainnetBlock1_fixedDifficulty1() {
    final GenesisConfigOptions genesisConfigOptions =
        GenesisConfigFile.fromConfig("{\"config\": {\"ethash\": {\"fixeddifficulty\":1}}}")
            .getConfigOptions();
    final ExecutionContextTestFixture executionContextTestFixture =
        ExecutionContextTestFixture.builder()
            .protocolSchedule(
                new ProtocolScheduleBuilder(
                        genesisConfigOptions,
                        BigInteger.valueOf(42),
                        ProtocolSpecAdapters.create(0, Function.identity()),
                        PrivacyParameters.DEFAULT,
                        false,
                        genesisConfigOptions.isQuorum())
                    .createProtocolSchedule())
            .build();

    final PoWSolver solver =
        new PoWSolver(
            Lists.newArrayList(BLOCK_1_NONCE),
            PoWHasher.ETHASH_LIGHT,
            false,
            Subscribers.none(),
            new EpochCalculator.DefaultEpochCalculator());

    final PendingTransactions pendingTransactions =
        new PendingTransactions(
            TransactionPoolConfiguration.DEFAULT_TX_RETENTION_HOURS,
            1,
            5,
            TestClock.fixed(),
            metricsSystem,
            executionContextTestFixture.getProtocolContext().getBlockchain()::getChainHeadHeader,
            TransactionPoolConfiguration.DEFAULT_PRICE_BUMP);

    final PoWBlockCreator blockCreator =
        new PoWBlockCreator(
            BLOCK_1_COINBASE,
            parent -> BLOCK_1_EXTRA_DATA,
            pendingTransactions,
            executionContextTestFixture.getProtocolContext(),
            executionContextTestFixture.getProtocolSchedule(),
            gasLimit -> gasLimit,
            solver,
            Wei.ZERO,
            0.8,
            executionContextTestFixture.getBlockchain().getChainHeadHeader());

    blockCreator.createBlock(BLOCK_1_TIMESTAMP);
    // If we weren't setting difficulty to 2^256-1 a difficulty of 1 would have caused a
    // IllegalArgumentException at the previous line, as 2^256 is 33 bytes.
  }

  @Test
  public void rewardBeneficiary_zeroReward_skipZeroRewardsFalse() {
    final GenesisConfigOptions genesisConfigOptions =
        GenesisConfigFile.fromConfig("{\"config\": {\"ethash\": {\"fixeddifficulty\":1}}}")
            .getConfigOptions();
    final ExecutionContextTestFixture executionContextTestFixture =
        ExecutionContextTestFixture.builder()
            .protocolSchedule(
                new ProtocolScheduleBuilder(
                        genesisConfigOptions,
                        BigInteger.valueOf(42),
                        ProtocolSpecAdapters.create(0, Function.identity()),
                        PrivacyParameters.DEFAULT,
                        false,
                        genesisConfigOptions.isQuorum())
                    .createProtocolSchedule())
            .build();

    final PoWSolver solver =
        new PoWSolver(
            Lists.newArrayList(BLOCK_1_NONCE),
            PoWHasher.ETHASH_LIGHT,
            false,
            Subscribers.none(),
            new EpochCalculator.DefaultEpochCalculator());

    final PendingTransactions pendingTransactions =
        new PendingTransactions(
            TransactionPoolConfiguration.DEFAULT_TX_RETENTION_HOURS,
            1,
            5,
            TestClock.fixed(),
            metricsSystem,
            executionContextTestFixture.getProtocolContext().getBlockchain()::getChainHeadHeader,
            TransactionPoolConfiguration.DEFAULT_PRICE_BUMP);

    final PoWBlockCreator blockCreator =
        new PoWBlockCreator(
            BLOCK_1_COINBASE,
            parent -> BLOCK_1_EXTRA_DATA,
            pendingTransactions,
            executionContextTestFixture.getProtocolContext(),
            executionContextTestFixture.getProtocolSchedule(),
            gasLimit -> gasLimit,
            solver,
            Wei.ZERO,
            0.8,
            executionContextTestFixture.getBlockchain().getChainHeadHeader());

    final MutableWorldState mutableWorldState =
        executionContextTestFixture.getStateArchive().getMutable();
    assertThat(mutableWorldState.get(BLOCK_1_COINBASE)).isNull();

    final ProcessableBlockHeader header =
        BlockHeaderBuilder.create()
            .parentHash(Hash.ZERO)
            .coinbase(BLOCK_1_COINBASE)
            .difficulty(Difficulty.ONE)
            .number(1)
            .gasLimit(1)
            .timestamp(1)
            .buildProcessableBlockHeader();

    blockCreator.rewardBeneficiary(
        mutableWorldState, header, Collections.emptyList(), Wei.ZERO, false);

    assertThat(mutableWorldState.get(BLOCK_1_COINBASE)).isNotNull();
    assertThat(mutableWorldState.get(BLOCK_1_COINBASE).getBalance()).isEqualTo(Wei.ZERO);
  }

  @Test
  public void rewardBeneficiary_zeroReward_skipZeroRewardsTrue() {
    final GenesisConfigOptions genesisConfigOptions =
        GenesisConfigFile.fromConfig("{\"config\": {\"ethash\": {\"fixeddifficulty\":1}}}")
            .getConfigOptions();
    final ExecutionContextTestFixture executionContextTestFixture =
        ExecutionContextTestFixture.builder()
            .protocolSchedule(
                new ProtocolScheduleBuilder(
                        genesisConfigOptions,
                        BigInteger.valueOf(42),
                        ProtocolSpecAdapters.create(0, Function.identity()),
                        PrivacyParameters.DEFAULT,
                        false,
                        genesisConfigOptions.isQuorum())
                    .createProtocolSchedule())
            .build();

    final PoWSolver solver =
        new PoWSolver(
            Lists.newArrayList(BLOCK_1_NONCE),
            PoWHasher.ETHASH_LIGHT,
            false,
            Subscribers.none(),
            new EpochCalculator.DefaultEpochCalculator());

    final PendingTransactions pendingTransactions =
        new PendingTransactions(
            TransactionPoolConfiguration.DEFAULT_TX_RETENTION_HOURS,
            1,
            5,
            TestClock.fixed(),
            metricsSystem,
            executionContextTestFixture.getProtocolContext().getBlockchain()::getChainHeadHeader,
            TransactionPoolConfiguration.DEFAULT_PRICE_BUMP);

    final PoWBlockCreator blockCreator =
        new PoWBlockCreator(
            BLOCK_1_COINBASE,
            parent -> BLOCK_1_EXTRA_DATA,
            pendingTransactions,
            executionContextTestFixture.getProtocolContext(),
            executionContextTestFixture.getProtocolSchedule(),
            gasLimit -> gasLimit,
            solver,
            Wei.ZERO,
            0.8,
            executionContextTestFixture.getBlockchain().getChainHeadHeader());

    final MutableWorldState mutableWorldState =
        executionContextTestFixture.getStateArchive().getMutable();
    assertThat(mutableWorldState.get(BLOCK_1_COINBASE)).isNull();

    final ProcessableBlockHeader header =
        BlockHeaderBuilder.create()
            .parentHash(Hash.ZERO)
            .coinbase(BLOCK_1_COINBASE)
            .difficulty(Difficulty.ONE)
            .number(1)
            .gasLimit(1)
            .timestamp(1)
            .buildProcessableBlockHeader();

    blockCreator.rewardBeneficiary(
        mutableWorldState, header, Collections.emptyList(), Wei.ZERO, true);

    assertThat(mutableWorldState.get(BLOCK_1_COINBASE)).isNull();
  }
}