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

import org.enterchain.enter.consensus.common.PoaQueryServiceImpl;
import org.enterchain.enter.consensus.common.bft.BftBlockInterface;
import org.enterchain.enter.consensus.common.bft.BftExtraData;
import org.enterchain.enter.crypto.NodeKey;
import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.plugin.data.Address;
import org.enterchain.enter.plugin.services.query.BftQueryService;

import java.util.Collection;
import java.util.Collections;

import org.apache.tuweni.bytes.Bytes32;

public class BftQueryServiceImpl extends PoaQueryServiceImpl implements BftQueryService {

  private final String consensusMechanismName;
  private final BftBlockInterface bftBlockInterface;

  public BftQueryServiceImpl(
      final BftBlockInterface blockInterface,
      final Blockchain blockchain,
      final NodeKey nodeKey,
      final String consensusMechanismName) {
    super(blockInterface, blockchain, nodeKey);
    this.bftBlockInterface = blockInterface;
    this.consensusMechanismName = consensusMechanismName;
  }

  @Override
  public int getRoundNumberFrom(final org.enterchain.enter.plugin.data.BlockHeader header) {
    final BlockHeader headerFromChain = getHeaderFromChain(header);
    final BftExtraData extraData = bftBlockInterface.getExtraData(headerFromChain);
    return extraData.getRound();
  }

  @Override
  public Collection<Address> getSignersFrom(
      final org.enterchain.enter.plugin.data.BlockHeader header) {
    final BlockHeader headerFromChain = getHeaderFromChain(header);
    return Collections.unmodifiableList(bftBlockInterface.getCommitters(headerFromChain));
  }

  @Override
  public String getConsensusMechanismName() {
    return consensusMechanismName;
  }

  private BlockHeader getHeaderFromChain(
      final org.enterchain.enter.plugin.data.BlockHeader header) {
    if (header instanceof BlockHeader) {
      return (BlockHeader) header;
    }

    final Hash blockHash = Hash.wrap(Bytes32.wrap(header.getBlockHash().toArray()));
    return getBlockchain().getBlockHeader(blockHash).orElseThrow();
  }
}
