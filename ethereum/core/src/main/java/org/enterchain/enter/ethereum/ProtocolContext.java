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
package org.enterchain.enter.ethereum;

import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.chain.GenesisState;
import org.enterchain.enter.ethereum.chain.MutableBlockchain;
import org.enterchain.enter.ethereum.worldstate.WorldStateArchive;

import java.util.function.BiFunction;

/**
 * Holds the mutable state used to track the current context of the protocol. This is primarily the
 * blockchain and world state archive, but can also hold arbitrary context required by a particular
 * consensus algorithm.
 */
public class ProtocolContext {
  private final MutableBlockchain blockchain;
  private final WorldStateArchive worldStateArchive;
  private final Object consensusState;

  public ProtocolContext(
      final MutableBlockchain blockchain,
      final WorldStateArchive worldStateArchive,
      final Object consensusState) {
    this.blockchain = blockchain;
    this.worldStateArchive = worldStateArchive;
    this.consensusState = consensusState;
  }

  public static ProtocolContext init(
      final MutableBlockchain blockchain,
      final WorldStateArchive worldStateArchive,
      final GenesisState genesisState,
      final BiFunction<Blockchain, WorldStateArchive, Object> consensusContextFactory) {
    if (blockchain.getChainHeadBlockNumber() < 1) {
      genesisState.writeStateTo(worldStateArchive.getMutable());
    }

    return new ProtocolContext(
        blockchain,
        worldStateArchive,
        consensusContextFactory.apply(blockchain, worldStateArchive));
  }

  public MutableBlockchain getBlockchain() {
    return blockchain;
  }

  public WorldStateArchive getWorldStateArchive() {
    return worldStateArchive;
  }

  public <C> C getConsensusState(final Class<C> klass) {
    return klass.cast(consensusState);
  }
}
