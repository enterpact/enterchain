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

import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.MiningParameters;
import org.enterchain.enter.ethereum.core.Transaction;
import org.enterchain.enter.ethereum.core.Wei;

import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;

public class NoopMiningCoordinator implements MiningCoordinator {

  private final MiningParameters miningParameters;

  public NoopMiningCoordinator(final MiningParameters miningParameters) {
    this.miningParameters = miningParameters;
  }

  @Override
  public void start() {}

  @Override
  public void stop() {}

  @Override
  public void awaitStop() {}

  @Override
  public boolean enable() {
    return false;
  }

  @Override
  public boolean disable() {
    return true;
  }

  @Override
  public boolean isMining() {
    return false;
  }

  @Override
  public Wei getMinTransactionGasPrice() {
    return miningParameters.getMinTransactionGasPrice();
  }

  @Override
  public void setExtraData(final Bytes extraData) {}

  @Override
  public Optional<Address> getCoinbase() {
    return miningParameters.getCoinbase();
  }

  @Override
  public Optional<Block> createBlock(
      final BlockHeader parentHeader,
      final List<Transaction> transactions,
      final List<BlockHeader> ommers) {
    return Optional.empty();
  }

  @Override
  public void changeTargetGasLimit(final Long targetGasLimit) {}
}
