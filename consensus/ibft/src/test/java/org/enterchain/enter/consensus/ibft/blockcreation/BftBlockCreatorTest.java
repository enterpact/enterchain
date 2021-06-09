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
package org.enterchain.enter.consensus.ibft.blockcreation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.enterchain.enter.consensus.common.bft.BftContextBuilder.setupContextWithBftExtraDataEncoder;
import static org.enterchain.enter.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryWorldStateArchive;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.enterchain.enter.config.GenesisConfigFile;
import org.enterchain.enter.consensus.common.bft.BftBlockHashing;
import org.enterchain.enter.consensus.common.bft.BftExtraData;
import org.enterchain.enter.consensus.common.bft.BftProtocolSchedule;
import org.enterchain.enter.consensus.common.bft.blockcreation.BftBlockCreator;
import org.enterchain.enter.consensus.ibft.IbftBlockHeaderValidationRulesetFactory;
import org.enterchain.enter.consensus.ibft.IbftExtraDataCodec;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.chain.MutableBlockchain;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.AddressHelpers;
import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.BlockHeaderTestFixture;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.eth.transactions.PendingTransactions;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPoolConfiguration;
import org.enterchain.enter.ethereum.mainnet.BlockHeaderValidator;
import org.enterchain.enter.ethereum.mainnet.HeaderValidationMode;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.metrics.noop.NoOpMetricsSystem;
import org.enterchain.enter.plugin.services.MetricsSystem;
import org.enterchain.enter.testutil.TestClock;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import org.apache.tuweni.bytes.Bytes;
import org.junit.Test;

public class BftBlockCreatorTest {
  private final MetricsSystem metricsSystem = new NoOpMetricsSystem();

  @Test
  public void createdBlockPassesValidationRulesAndHasAppropriateHashAndMixHash() {
    // Construct a parent block.
    final BlockHeaderTestFixture blockHeaderBuilder = new BlockHeaderTestFixture();
    blockHeaderBuilder.gasLimit(5000); // required to pass validation rule checks.
    final BlockHeader parentHeader = blockHeaderBuilder.buildHeader();
    final Optional<BlockHeader> optionalHeader = Optional.of(parentHeader);

    // Construct a block chain and world state
    final MutableBlockchain blockchain = mock(MutableBlockchain.class);
    when(blockchain.getChainHeadHash()).thenReturn(parentHeader.getHash());
    when(blockchain.getBlockHeader(any())).thenReturn(optionalHeader);
    final BlockHeader blockHeader = mock(BlockHeader.class);
    when(blockHeader.getBaseFee()).thenReturn(Optional.empty());
    when(blockchain.getChainHeadHeader()).thenReturn(blockHeader);

    final List<Address> initialValidatorList = Lists.newArrayList();
    for (int i = 0; i < 4; i++) {
      initialValidatorList.add(AddressHelpers.ofValue(i));
    }

    final IbftExtraDataCodec bftExtraDataEncoder = new IbftExtraDataCodec();
    final ProtocolSchedule protocolSchedule =
        BftProtocolSchedule.create(
            GenesisConfigFile.fromConfig("{\"config\": {\"spuriousDragonBlock\":0}}")
                .getConfigOptions(),
            IbftBlockHeaderValidationRulesetFactory::blockHeaderValidator,
            bftExtraDataEncoder);
    final ProtocolContext protContext =
        new ProtocolContext(
            blockchain,
            createInMemoryWorldStateArchive(),
            setupContextWithBftExtraDataEncoder(initialValidatorList, bftExtraDataEncoder));

    final PendingTransactions pendingTransactions =
        new PendingTransactions(
            TransactionPoolConfiguration.DEFAULT_TX_RETENTION_HOURS,
            1,
            5,
            TestClock.fixed(),
            metricsSystem,
            blockchain::getChainHeadHeader,
            TransactionPoolConfiguration.DEFAULT_PRICE_BUMP);

    final BftBlockCreator blockCreator =
        new BftBlockCreator(
            initialValidatorList.get(0),
            parent ->
                bftExtraDataEncoder.encode(
                    new BftExtraData(
                        Bytes.wrap(new byte[32]),
                        Collections.emptyList(),
                        Optional.empty(),
                        0,
                        initialValidatorList)),
            pendingTransactions,
            protContext,
            protocolSchedule,
            parentGasLimit -> parentGasLimit,
            Wei.ZERO,
            0.8,
            parentHeader,
            initialValidatorList.get(0),
            bftExtraDataEncoder);

    final int secondsBetweenBlocks = 1;
    final Block block = blockCreator.createBlock(parentHeader.getTimestamp() + 1);

    final BlockHeaderValidator rules =
        IbftBlockHeaderValidationRulesetFactory.blockHeaderValidator(secondsBetweenBlocks).build();

    // NOTE: The header will not contain commit seals, so can only do light validation on header.
    final boolean validationResult =
        rules.validateHeader(
            block.getHeader(), parentHeader, protContext, HeaderValidationMode.LIGHT);

    assertThat(validationResult).isTrue();

    final BlockHeader header = block.getHeader();
    final BftExtraData extraData = bftExtraDataEncoder.decode(header);
    assertThat(block.getHash())
        .isEqualTo(
            new BftBlockHashing(bftExtraDataEncoder)
                .calculateDataHashForCommittedSeal(header, extraData));
  }
}
