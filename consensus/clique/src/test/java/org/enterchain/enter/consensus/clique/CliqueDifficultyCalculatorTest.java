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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.enterchain.enter.consensus.common.VoteProposer;
import org.enterchain.enter.consensus.common.VoteTally;
import org.enterchain.enter.consensus.common.VoteTallyCache;
import org.enterchain.enter.crypto.KeyPair;
import org.enterchain.enter.crypto.SignatureAlgorithmFactory;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.AddressHelpers;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.BlockHeaderTestFixture;
import org.enterchain.enter.ethereum.core.Util;

import java.math.BigInteger;
import java.util.List;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

public class CliqueDifficultyCalculatorTest {

  private final KeyPair proposerKeyPair = SignatureAlgorithmFactory.getInstance().generateKeyPair();
  private Address localAddr;

  private final List<Address> validatorList = Lists.newArrayList();
  private ProtocolContext cliqueProtocolContext;
  private BlockHeaderTestFixture blockHeaderBuilder;
  private final CliqueBlockInterface blockInterface = new CliqueBlockInterface();

  @Before
  public void setup() {
    localAddr = Util.publicKeyToAddress(proposerKeyPair.getPublicKey());

    validatorList.add(localAddr);
    validatorList.add(AddressHelpers.calculateAddressWithRespectTo(localAddr, 1));

    final VoteTallyCache voteTallyCache = mock(VoteTallyCache.class);
    when(voteTallyCache.getVoteTallyAfterBlock(any())).thenReturn(new VoteTally(validatorList));
    final VoteProposer voteProposer = new VoteProposer();

    final CliqueContext cliqueContext =
        new CliqueContext(voteTallyCache, voteProposer, null, blockInterface);
    cliqueProtocolContext = new ProtocolContext(null, null, cliqueContext);
    blockHeaderBuilder = new BlockHeaderTestFixture();
  }

  @Test
  public void inTurnValidatorProducesDifficultyOfTwo() {
    final CliqueDifficultyCalculator calculator = new CliqueDifficultyCalculator(localAddr);

    final BlockHeader parentHeader = blockHeaderBuilder.number(1).buildHeader();

    assertThat(calculator.nextDifficulty(0, parentHeader, cliqueProtocolContext))
        .isEqualTo(BigInteger.valueOf(2));
  }

  @Test
  public void outTurnValidatorProducesDifficultyOfOne() {
    final CliqueDifficultyCalculator calculator = new CliqueDifficultyCalculator(localAddr);

    final BlockHeader parentHeader = blockHeaderBuilder.number(2).buildHeader();

    assertThat(calculator.nextDifficulty(0, parentHeader, cliqueProtocolContext))
        .isEqualTo(BigInteger.valueOf(1));
  }
}
