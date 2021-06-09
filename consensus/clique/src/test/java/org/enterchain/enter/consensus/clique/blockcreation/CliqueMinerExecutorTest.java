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
package org.enterchain.enter.consensus.clique.blockcreation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.enterchain.enter.config.GenesisConfigFile;
import org.enterchain.enter.config.GenesisConfigOptions;
import org.enterchain.enter.consensus.clique.CliqueBlockHeaderFunctions;
import org.enterchain.enter.consensus.clique.CliqueBlockInterface;
import org.enterchain.enter.consensus.clique.CliqueContext;
import org.enterchain.enter.consensus.clique.CliqueExtraData;
import org.enterchain.enter.consensus.clique.CliqueProtocolSchedule;
import org.enterchain.enter.consensus.common.EpochManager;
import org.enterchain.enter.consensus.common.VoteProposer;
import org.enterchain.enter.consensus.common.VoteTally;
import org.enterchain.enter.consensus.common.VoteTallyCache;
import org.enterchain.enter.crypto.NodeKey;
import org.enterchain.enter.crypto.NodeKeyUtils;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.blockcreation.GasLimitCalculator;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.AddressHelpers;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.BlockHeaderTestFixture;
import org.enterchain.enter.ethereum.core.MiningParameters;
import org.enterchain.enter.ethereum.core.Util;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.eth.transactions.PendingTransactions;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.enterchain.enter.metrics.noop.NoOpMetricsSystem;
import org.enterchain.enter.plugin.services.MetricsSystem;
import org.enterchain.enter.testutil.TestClock;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import com.google.common.collect.Lists;
import org.apache.tuweni.bytes.Bytes;
import org.junit.Before;
import org.junit.Test;

public class CliqueMinerExecutorTest {

  private static final int EPOCH_LENGTH = 10;
  private static final GenesisConfigOptions GENESIS_CONFIG_OPTIONS =
      GenesisConfigFile.fromConfig("{}").getConfigOptions();
  private final NodeKey proposerNodeKey = NodeKeyUtils.generate();
  private final Random random = new Random(21341234L);
  private Address localAddress;
  private final List<Address> validatorList = Lists.newArrayList();
  private ProtocolContext cliqueProtocolContext;
  private BlockHeaderTestFixture blockHeaderBuilder;
  private final MetricsSystem metricsSystem = new NoOpMetricsSystem();
  private final CliqueBlockInterface blockInterface = new CliqueBlockInterface();

  @Before
  public void setup() {
    localAddress = Util.publicKeyToAddress(proposerNodeKey.getPublicKey());
    validatorList.add(localAddress);
    validatorList.add(AddressHelpers.calculateAddressWithRespectTo(localAddress, 1));
    validatorList.add(AddressHelpers.calculateAddressWithRespectTo(localAddress, 2));
    validatorList.add(AddressHelpers.calculateAddressWithRespectTo(localAddress, 3));

    final VoteTallyCache voteTallyCache = mock(VoteTallyCache.class);
    when(voteTallyCache.getVoteTallyAfterBlock(any())).thenReturn(new VoteTally(validatorList));
    final VoteProposer voteProposer = new VoteProposer();

    final CliqueContext cliqueContext =
        new CliqueContext(voteTallyCache, voteProposer, null, blockInterface);
    cliqueProtocolContext = new ProtocolContext(null, null, cliqueContext);
    blockHeaderBuilder = new BlockHeaderTestFixture();
  }

  @Test
  public void extraDataCreatedOnEpochBlocksContainsValidators() {
    final Bytes vanityData = generateRandomVanityData();

    final CliqueMinerExecutor executor =
        new CliqueMinerExecutor(
            cliqueProtocolContext,
            CliqueProtocolSchedule.create(GENESIS_CONFIG_OPTIONS, proposerNodeKey, false),
            new PendingTransactions(
                TransactionPoolConfiguration.DEFAULT_TX_RETENTION_HOURS,
                1,
                5,
                TestClock.fixed(),
                metricsSystem,
                CliqueMinerExecutorTest::mockBlockHeader,
                TransactionPoolConfiguration.DEFAULT_PRICE_BUMP),
            proposerNodeKey,
            new MiningParameters(AddressHelpers.ofValue(1), Wei.ZERO, vanityData, false),
            mock(CliqueBlockScheduler.class),
            new EpochManager(EPOCH_LENGTH),
            GasLimitCalculator.constant());

    // NOTE: Passing in the *parent* block, so must be 1 less than EPOCH
    final BlockHeader header = blockHeaderBuilder.number(EPOCH_LENGTH - 1).buildHeader();

    final Bytes extraDataBytes = executor.calculateExtraData(header);

    final CliqueExtraData cliqueExtraData =
        CliqueExtraData.decode(
            blockHeaderBuilder
                .number(EPOCH_LENGTH)
                .extraData(extraDataBytes)
                .blockHeaderFunctions(new CliqueBlockHeaderFunctions())
                .buildHeader());

    assertThat(cliqueExtraData.getVanityData()).isEqualTo(vanityData);
    assertThat(cliqueExtraData.getValidators())
        .containsExactly(validatorList.toArray(new Address[0]));
  }

  @Test
  public void extraDataForNonEpochBlocksDoesNotContainValidaors() {
    final Bytes vanityData = generateRandomVanityData();

    final CliqueMinerExecutor executor =
        new CliqueMinerExecutor(
            cliqueProtocolContext,
            CliqueProtocolSchedule.create(GENESIS_CONFIG_OPTIONS, proposerNodeKey, false),
            new PendingTransactions(
                TransactionPoolConfiguration.DEFAULT_TX_RETENTION_HOURS,
                1,
                5,
                TestClock.fixed(),
                metricsSystem,
                CliqueMinerExecutorTest::mockBlockHeader,
                TransactionPoolConfiguration.DEFAULT_PRICE_BUMP),
            proposerNodeKey,
            new MiningParameters(AddressHelpers.ofValue(1), Wei.ZERO, vanityData, false),
            mock(CliqueBlockScheduler.class),
            new EpochManager(EPOCH_LENGTH),
            GasLimitCalculator.constant());

    // Parent block was epoch, so the next block should contain no validators.
    final BlockHeader header = blockHeaderBuilder.number(EPOCH_LENGTH).buildHeader();

    final Bytes extraDataBytes = executor.calculateExtraData(header);

    final CliqueExtraData cliqueExtraData =
        CliqueExtraData.decode(
            blockHeaderBuilder
                .number(EPOCH_LENGTH)
                .extraData(extraDataBytes)
                .blockHeaderFunctions(new CliqueBlockHeaderFunctions())
                .buildHeader());

    assertThat(cliqueExtraData.getVanityData()).isEqualTo(vanityData);
    assertThat(cliqueExtraData.getValidators()).isEqualTo(Lists.newArrayList());
  }

  @Test
  public void shouldUseLatestVanityData() {
    final Bytes initialVanityData = generateRandomVanityData();
    final Bytes modifiedVanityData = generateRandomVanityData();

    final CliqueMinerExecutor executor =
        new CliqueMinerExecutor(
            cliqueProtocolContext,
            CliqueProtocolSchedule.create(GENESIS_CONFIG_OPTIONS, proposerNodeKey, false),
            new PendingTransactions(
                TransactionPoolConfiguration.DEFAULT_TX_RETENTION_HOURS,
                1,
                5,
                TestClock.fixed(),
                metricsSystem,
                CliqueMinerExecutorTest::mockBlockHeader,
                TransactionPoolConfiguration.DEFAULT_PRICE_BUMP),
            proposerNodeKey,
            new MiningParameters(AddressHelpers.ofValue(1), Wei.ZERO, initialVanityData, false),
            mock(CliqueBlockScheduler.class),
            new EpochManager(EPOCH_LENGTH),
            GasLimitCalculator.constant());

    executor.setExtraData(modifiedVanityData);
    final Bytes extraDataBytes = executor.calculateExtraData(blockHeaderBuilder.buildHeader());

    final CliqueExtraData cliqueExtraData =
        CliqueExtraData.decode(
            blockHeaderBuilder
                .number(EPOCH_LENGTH)
                .extraData(extraDataBytes)
                .blockHeaderFunctions(new CliqueBlockHeaderFunctions())
                .buildHeader());
    assertThat(cliqueExtraData.getVanityData()).isEqualTo(modifiedVanityData);
  }

  private static BlockHeader mockBlockHeader() {
    final BlockHeader blockHeader = mock(BlockHeader.class);
    when(blockHeader.getBaseFee()).thenReturn(Optional.empty());
    return blockHeader;
  }

  private Bytes generateRandomVanityData() {
    final byte[] vanityData = new byte[32];
    random.nextBytes(vanityData);
    return Bytes.wrap(vanityData);
  }
}
