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
package org.enterchain.enter.consensus.common.bft.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.enterchain.enter.consensus.common.bft.BftBlockHeaderFunctions;
import org.enterchain.enter.consensus.common.bft.BftBlockInterface;
import org.enterchain.enter.consensus.common.bft.BftExtraData;
import org.enterchain.enter.consensus.common.bft.BftExtraDataCodec;
import org.enterchain.enter.crypto.NodeKey;
import org.enterchain.enter.crypto.NodeKeyUtils;
import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.BlockHeaderTestFixture;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.core.NonBesuBlockHeader;
import org.enterchain.enter.ethereum.core.Util;
import org.enterchain.enter.plugin.services.query.BftQueryService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.apache.tuweni.bytes.Bytes;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BftQueryServiceImplTest {

  @Mock private Blockchain blockchain;

  @Mock private BftExtraDataCodec bftExtraDataCodec;

  @Mock private BftBlockInterface bftBlockInterface;

  private final List<NodeKey> validatorKeys =
      Lists.newArrayList(NodeKeyUtils.generate(), NodeKeyUtils.generate());

  private final List<NodeKey> signingKeys = Lists.newArrayList(validatorKeys.get(0));

  private BlockHeader blockHeader;

  @Before
  public void setup() {
    final BlockHeaderTestFixture blockHeaderTestFixture = new BlockHeaderTestFixture();
    blockHeaderTestFixture.number(1); // can't be genesis block (due to extradata serialisation)
    blockHeaderTestFixture.blockHeaderFunctions(
        BftBlockHeaderFunctions.forOnChainBlock(bftExtraDataCodec));

    blockHeader = blockHeaderTestFixture.buildHeader();
  }

  @Test
  public void roundNumberFromBlockIsReturned() {
    final BftQueryService service =
        new BftQueryServiceImpl(bftBlockInterface, blockchain, null, null);
    final int roundNumberInBlock = 5;
    final BftExtraData extraData =
        new BftExtraData(Bytes.EMPTY, List.of(), Optional.empty(), roundNumberInBlock, List.of());
    when(bftBlockInterface.getExtraData(blockHeader)).thenReturn(extraData);

    assertThat(service.getRoundNumberFrom(blockHeader)).isEqualTo(roundNumberInBlock);
  }

  @Test
  public void getRoundNumberThrowsIfBlockIsNotOnTheChain() {
    final NonBesuBlockHeader header = new NonBesuBlockHeader(Hash.EMPTY, Bytes.EMPTY);

    final BftQueryService service =
        new BftQueryServiceImpl(new BftBlockInterface(bftExtraDataCodec), blockchain, null, null);
    assertThatExceptionOfType(NoSuchElementException.class)
        .isThrownBy(() -> service.getRoundNumberFrom(header));
  }

  @Test
  public void getSignersReturnsAddressesOfSignersInBlock() {
    final BftQueryService service =
        new BftQueryServiceImpl(bftBlockInterface, blockchain, null, null);

    final List<Address> signers =
        signingKeys.stream()
            .map(nodeKey -> Util.publicKeyToAddress(nodeKey.getPublicKey()))
            .collect(Collectors.toList());
    when(bftBlockInterface.getCommitters(any())).thenReturn(signers);

    assertThat(service.getSignersFrom(blockHeader)).containsExactlyElementsOf(signers);
  }

  @Test
  public void getSignersThrowsIfBlockIsNotOnTheChain() {
    final NonBesuBlockHeader header = new NonBesuBlockHeader(Hash.EMPTY, Bytes.EMPTY);

    final BftQueryService service =
        new BftQueryServiceImpl(bftBlockInterface, blockchain, null, null);
    assertThatExceptionOfType(NoSuchElementException.class)
        .isThrownBy(() -> service.getSignersFrom(header));
  }

  @Test
  public void consensusMechanismNameReturnedIsSameAsThatPassedDuringCreation() {
    final BftQueryService service =
        new BftQueryServiceImpl(
            new BftBlockInterface(bftExtraDataCodec), blockchain, null, "consensusMechanism");
    assertThat(service.getConsensusMechanismName()).isEqualTo("consensusMechanism");
  }
}
