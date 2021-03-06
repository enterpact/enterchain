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
package org.enterchain.enter.consensus.common.bft.statemachine;

import org.enterchain.enter.consensus.common.VoteTallyCache;
import org.enterchain.enter.consensus.common.bft.BftHelpers;
import org.enterchain.enter.consensus.common.bft.BlockTimer;
import org.enterchain.enter.consensus.common.bft.ConsensusRoundIdentifier;
import org.enterchain.enter.consensus.common.bft.RoundTimer;
import org.enterchain.enter.consensus.common.bft.blockcreation.BftBlockCreatorFactory;
import org.enterchain.enter.consensus.common.bft.blockcreation.ProposerSelector;
import org.enterchain.enter.consensus.common.bft.network.ValidatorMulticaster;
import org.enterchain.enter.crypto.NodeKey;
import org.enterchain.enter.ethereum.core.Address;

import java.time.Clock;
import java.util.Collection;

/** This is the full data set, or context, required for many of the aspects of BFT workflows. */
public class BftFinalState {
  private final VoteTallyCache voteTallyCache;
  private final NodeKey nodeKey;
  private final Address localAddress;
  private final ProposerSelector proposerSelector;
  private final RoundTimer roundTimer;
  private final BlockTimer blockTimer;
  private final BftBlockCreatorFactory blockCreatorFactory;
  private final Clock clock;
  private final ValidatorMulticaster validatorMulticaster;

  public BftFinalState(
      final VoteTallyCache voteTallyCache,
      final NodeKey nodeKey,
      final Address localAddress,
      final ProposerSelector proposerSelector,
      final ValidatorMulticaster validatorMulticaster,
      final RoundTimer roundTimer,
      final BlockTimer blockTimer,
      final BftBlockCreatorFactory blockCreatorFactory,
      final Clock clock) {
    this.voteTallyCache = voteTallyCache;
    this.nodeKey = nodeKey;
    this.localAddress = localAddress;
    this.proposerSelector = proposerSelector;
    this.roundTimer = roundTimer;
    this.blockTimer = blockTimer;
    this.blockCreatorFactory = blockCreatorFactory;
    this.clock = clock;
    this.validatorMulticaster = validatorMulticaster;
  }

  public int getQuorum() {
    return BftHelpers.calculateRequiredValidatorQuorum(getValidators().size());
  }

  public Collection<Address> getValidators() {
    return voteTallyCache.getVoteTallyAtHead().getValidators();
  }

  public NodeKey getNodeKey() {
    return nodeKey;
  }

  public Address getLocalAddress() {
    return localAddress;
  }

  public boolean isLocalNodeProposerForRound(final ConsensusRoundIdentifier roundIdentifier) {
    return getProposerForRound(roundIdentifier).equals(localAddress);
  }

  public boolean isLocalNodeValidator() {
    return getValidators().contains(localAddress);
  }

  public RoundTimer getRoundTimer() {
    return roundTimer;
  }

  public BlockTimer getBlockTimer() {
    return blockTimer;
  }

  public BftBlockCreatorFactory getBlockCreatorFactory() {
    return blockCreatorFactory;
  }

  public Address getProposerForRound(final ConsensusRoundIdentifier roundIdentifier) {
    return proposerSelector.selectProposerForRound(roundIdentifier);
  }

  public ValidatorMulticaster getValidatorMulticaster() {
    return validatorMulticaster;
  }

  public Clock getClock() {
    return clock;
  }
}
