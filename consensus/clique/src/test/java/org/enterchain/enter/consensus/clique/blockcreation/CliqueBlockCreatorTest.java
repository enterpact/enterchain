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

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.enterchain.enter.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryBlockchain;
import static org.enterchain.enter.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryWorldStateArchive;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.enterchain.enter.config.GenesisConfigFile;
import org.enterchain.enter.consensus.clique.CliqueBlockInterface;
import org.enterchain.enter.consensus.clique.CliqueContext;
import org.enterchain.enter.consensus.clique.CliqueExtraData;
import org.enterchain.enter.consensus.clique.CliqueHelpers;
import org.enterchain.enter.consensus.clique.CliqueProtocolSchedule;
import org.enterchain.enter.consensus.clique.TestHelpers;
import org.enterchain.enter.consensus.common.EpochManager;
import org.enterchain.enter.consensus.common.VoteProposer;
import org.enterchain.enter.consensus.common.VoteTally;
import org.enterchain.enter.consensus.common.VoteTallyCache;
import org.enterchain.enter.crypto.KeyPair;
import org.enterchain.enter.crypto.NodeKey;
import org.enterchain.enter.crypto.NodeKeyUtils;
import org.enterchain.enter.crypto.SignatureAlgorithmFactory;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.chain.GenesisState;
import org.enterchain.enter.ethereum.chain.MutableBlockchain;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.AddressHelpers;
import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.BlockBody;
import org.enterchain.enter.ethereum.core.BlockHeaderTestFixture;
import org.enterchain.enter.ethereum.core.Util;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.eth.transactions.PendingTransactions;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.worldstate.WorldStateArchive;
import org.enterchain.enter.metrics.noop.NoOpMetricsSystem;
import org.enterchain.enter.plugin.services.MetricsSystem;
import org.enterchain.enter.testutil.TestClock;

import java.util.List;

import com.google.common.collect.Lists;
import org.apache.tuweni.bytes.Bytes;
import org.assertj.core.api.Java6Assertions;
import org.junit.Before;
import org.junit.Test;

public class CliqueBlockCreatorTest {

  private final NodeKey proposerNodeKey = NodeKeyUtils.generate();
  private final Address proposerAddress = Util.publicKeyToAddress(proposerNodeKey.getPublicKey());
  private final KeyPair otherKeyPair = SignatureAlgorithmFactory.getInstance().generateKeyPair();
  private final List<Address> validatorList = Lists.newArrayList();
  private final MetricsSystem metricsSystem = new NoOpMetricsSystem();
  private final CliqueBlockInterface blockInterface = new CliqueBlockInterface();

  private ProtocolSchedule protocolSchedule;
  private final WorldStateArchive stateArchive = createInMemoryWorldStateArchive();

  private MutableBlockchain blockchain;
  private ProtocolContext protocolContext;
  private VoteProposer voteProposer;
  private EpochManager epochManager;

  @Before
  public void setup() {
    protocolSchedule =
        CliqueProtocolSchedule.create(
            GenesisConfigFile.DEFAULT.getConfigOptions(), proposerNodeKey, false);

    final Address otherAddress = Util.publicKeyToAddress(otherKeyPair.getPublicKey());
    validatorList.add(otherAddress);

    final VoteTallyCache voteTallyCache = mock(VoteTallyCache.class);
    when(voteTallyCache.getVoteTallyAfterBlock(any())).thenReturn(new VoteTally(validatorList));
    voteProposer = new VoteProposer();
    final CliqueContext cliqueContext =
        new CliqueContext(voteTallyCache, voteProposer, null, blockInterface);

    final Block genesis =
        GenesisState.fromConfig(GenesisConfigFile.mainnet(), protocolSchedule).getBlock();
    blockchain = createInMemoryBlockchain(genesis);
    protocolContext = new ProtocolContext(blockchain, stateArchive, cliqueContext);
    epochManager = new EpochManager(10);

    // Add a block above the genesis
    final BlockHeaderTestFixture headerTestFixture = new BlockHeaderTestFixture();
    headerTestFixture.number(1).parentHash(genesis.getHeader().getHash());
    final Block emptyBlock =
        new Block(
            TestHelpers.createCliqueSignedBlockHeader(
                headerTestFixture, otherKeyPair, validatorList),
            new BlockBody(Lists.newArrayList(), Lists.newArrayList()));
    blockchain.appendBlock(emptyBlock, Lists.newArrayList());
  }

  @Test
  public void proposerAddressCanBeExtractFromAConstructedBlock() {

    final Bytes extraData =
        CliqueExtraData.createWithoutProposerSeal(Bytes.wrap(new byte[32]), validatorList);

    final Address coinbase = AddressHelpers.ofValue(1);
    final CliqueBlockCreator blockCreator =
        new CliqueBlockCreator(
            coinbase,
            parent -> extraData,
            new PendingTransactions(
                TransactionPoolConfiguration.DEFAULT_TX_RETENTION_HOURS,
                5,
                5,
                TestClock.fixed(),
                metricsSystem,
                blockchain::getChainHeadHeader,
                TransactionPoolConfiguration.DEFAULT_PRICE_BUMP),
            protocolContext,
            protocolSchedule,
            gasLimit -> gasLimit,
            proposerNodeKey,
            Wei.ZERO,
            0.8,
            blockchain.getChainHeadHeader(),
            epochManager);

    final Block createdBlock = blockCreator.createBlock(5L);

    Java6Assertions.assertThat(CliqueHelpers.getProposerOfBlock(createdBlock.getHeader()))
        .isEqualTo(proposerAddress);
  }

  @Test
  public void insertsValidVoteIntoConstructedBlock() {
    final Bytes extraData =
        CliqueExtraData.createWithoutProposerSeal(Bytes.wrap(new byte[32]), validatorList);
    final Address a1 = Address.fromHexString("5");
    voteProposer.auth(a1);
    final Address coinbase = AddressHelpers.ofValue(1);

    final CliqueBlockCreator blockCreator =
        new CliqueBlockCreator(
            coinbase,
            parent -> extraData,
            new PendingTransactions(
                TransactionPoolConfiguration.DEFAULT_TX_RETENTION_HOURS,
                5,
                5,
                TestClock.fixed(),
                metricsSystem,
                blockchain::getChainHeadHeader,
                TransactionPoolConfiguration.DEFAULT_PRICE_BUMP),
            protocolContext,
            protocolSchedule,
            gasLimit -> gasLimit,
            proposerNodeKey,
            Wei.ZERO,
            0.8,
            blockchain.getChainHeadHeader(),
            epochManager);

    final Block createdBlock = blockCreator.createBlock(0L);
    assertThat(createdBlock.getHeader().getNonce()).isEqualTo(CliqueBlockInterface.ADD_NONCE);
    assertThat(createdBlock.getHeader().getCoinbase()).isEqualTo(a1);
  }

  @Test
  public void insertsNoVoteWhenAuthInValidators() {
    final Bytes extraData =
        CliqueExtraData.createWithoutProposerSeal(Bytes.wrap(new byte[32]), validatorList);
    final Address a1 = Util.publicKeyToAddress(otherKeyPair.getPublicKey());
    voteProposer.auth(a1);
    final Address coinbase = AddressHelpers.ofValue(1);

    final CliqueBlockCreator blockCreator =
        new CliqueBlockCreator(
            coinbase,
            parent -> extraData,
            new PendingTransactions(
                TransactionPoolConfiguration.DEFAULT_TX_RETENTION_HOURS,
                5,
                5,
                TestClock.fixed(),
                metricsSystem,
                blockchain::getChainHeadHeader,
                TransactionPoolConfiguration.DEFAULT_PRICE_BUMP),
            protocolContext,
            protocolSchedule,
            gasLimit -> gasLimit,
            proposerNodeKey,
            Wei.ZERO,
            0.8,
            blockchain.getChainHeadHeader(),
            epochManager);

    final Block createdBlock = blockCreator.createBlock(0L);
    assertThat(createdBlock.getHeader().getNonce()).isEqualTo(CliqueBlockInterface.DROP_NONCE);
    assertThat(createdBlock.getHeader().getCoinbase()).isEqualTo(Address.fromHexString("0"));
  }

  @Test
  public void insertsNoVoteWhenAtEpoch() {
    // ensure that the next block is epoch
    epochManager = new EpochManager(1);

    final Bytes extraData =
        CliqueExtraData.createWithoutProposerSeal(Bytes.wrap(new byte[32]), validatorList);
    final Address a1 = Address.fromHexString("5");
    voteProposer.auth(a1);
    final Address coinbase = AddressHelpers.ofValue(1);

    final CliqueBlockCreator blockCreator =
        new CliqueBlockCreator(
            coinbase,
            parent -> extraData,
            new PendingTransactions(
                TransactionPoolConfiguration.DEFAULT_TX_RETENTION_HOURS,
                5,
                5,
                TestClock.fixed(),
                metricsSystem,
                blockchain::getChainHeadHeader,
                TransactionPoolConfiguration.DEFAULT_PRICE_BUMP),
            protocolContext,
            protocolSchedule,
            gasLimit -> gasLimit,
            proposerNodeKey,
            Wei.ZERO,
            0.8,
            blockchain.getChainHeadHeader(),
            epochManager);

    final Block createdBlock = blockCreator.createBlock(0L);
    assertThat(createdBlock.getHeader().getNonce()).isEqualTo(CliqueBlockInterface.DROP_NONCE);
    assertThat(createdBlock.getHeader().getCoinbase()).isEqualTo(Address.fromHexString("0"));
  }
}
