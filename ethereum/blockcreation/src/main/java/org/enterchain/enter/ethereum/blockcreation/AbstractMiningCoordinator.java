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

import org.enterchain.enter.ethereum.chain.BlockAddedEvent;
import org.enterchain.enter.ethereum.chain.BlockAddedObserver;
import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.chain.MinedBlockObserver;
import org.enterchain.enter.ethereum.chain.PoWObserver;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.Transaction;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.eth.sync.state.SyncState;
import org.enterchain.enter.util.Subscribers;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.tuweni.bytes.Bytes;

public abstract class AbstractMiningCoordinator<
        M extends BlockMiner<? extends AbstractBlockCreator>>
    implements BlockAddedObserver, MiningCoordinator {

  private enum State {
    IDLE,
    RUNNING,
    STOPPED
  }

  private final Subscribers<MinedBlockObserver> minedBlockObservers = Subscribers.create();
  private final Subscribers<PoWObserver> ethHashObservers = Subscribers.create();
  private final AbstractMinerExecutor<M> executor;
  private final SyncState syncState;
  protected final Blockchain blockchain;

  private State state = State.IDLE;
  private boolean isEnabled = false;
  protected Optional<M> currentRunningMiner = Optional.empty();

  protected AbstractMiningCoordinator(
      final Blockchain blockchain,
      final AbstractMinerExecutor<M> executor,
      final SyncState syncState) {
    this.executor = executor;
    this.blockchain = blockchain;
    this.syncState = syncState;
    this.blockchain.observeBlockAdded(this);
    syncState.subscribeInSync(this::inSyncChanged);
  }

  @Override
  public Optional<Block> createBlock(
      final BlockHeader parentHeader,
      final List<Transaction> transactions,
      final List<BlockHeader> ommers) {
    final M miner = executor.createMiner(minedBlockObservers, ethHashObservers, parentHeader);
    return Optional.of(miner.createBlock(parentHeader, transactions, ommers));
  }

  @Override
  public void start() {
    synchronized (this) {
      if (state != State.IDLE) {
        return;
      }
      state = State.RUNNING;
      startMiningIfPossible();
    }
  }

  @Override
  public void stop() {
    synchronized (this) {
      if (state != State.RUNNING) {
        return;
      }
      state = State.STOPPED;
      haltCurrentMiningOperation();
      executor.shutDown();
    }
  }

  @Override
  public void awaitStop() throws InterruptedException {
    executor.awaitShutdown();
  }

  @Override
  public boolean enable() {
    synchronized (this) {
      if (isEnabled) {
        return true;
      }
      isEnabled = true;
      startMiningIfPossible();
    }
    return true;
  }

  @Override
  public boolean disable() {
    synchronized (this) {
      if (!isEnabled) {
        return false;
      }
      isEnabled = false;
      haltCurrentMiningOperation();
    }
    return false;
  }

  @Override
  public boolean isMining() {
    synchronized (this) {
      return currentRunningMiner.isPresent();
    }
  }

  private synchronized boolean startMiningIfPossible() {
    if ((state != State.RUNNING) || !isEnabled || !syncState.isInSync() || isMining()) {
      return false;
    }

    startAsyncMiningOperation();
    return true;
  }

  private void startAsyncMiningOperation() {
    final BlockHeader parentHeader = blockchain.getChainHeadHeader();
    currentRunningMiner =
        executor.startAsyncMining(minedBlockObservers, ethHashObservers, parentHeader);
  }

  private synchronized boolean haltCurrentMiningOperation() {
    final AtomicBoolean wasHalted = new AtomicBoolean(false);
    currentRunningMiner.ifPresent(
        (miner) -> {
          haltMiner(miner);
          wasHalted.set(true);
        });
    currentRunningMiner = Optional.empty();
    return wasHalted.get();
  }

  protected void haltMiner(final M miner) {
    miner.cancel();
  }

  @Override
  public void onBlockAdded(final BlockAddedEvent event) {
    synchronized (this) {
      if (event.isNewCanonicalHead()
          && newChainHeadInvalidatesMiningOperation(event.getBlock().getHeader())) {
        haltCurrentMiningOperation();
        startMiningIfPossible();
      }
    }
  }

  void inSyncChanged(final boolean inSync) {
    synchronized (this) {
      if (inSync && startMiningIfPossible()) {
        onResumeMining();
      }
      if (!inSync && haltCurrentMiningOperation()) {
        onPauseMining();
      }
    }
  }

  public void addMinedBlockObserver(final MinedBlockObserver obs) {
    minedBlockObservers.subscribe(obs);
  }

  @Override
  public void addEthHashObserver(final PoWObserver obs) {
    ethHashObservers.subscribe(obs);
  }

  @Override
  public Wei getMinTransactionGasPrice() {
    return executor.getMinTransactionGasPrice();
  }

  @Override
  public void setExtraData(final Bytes extraData) {
    executor.setExtraData(extraData);
  }

  @Override
  public Optional<Address> getCoinbase() {
    return executor.getCoinbase();
  }

  protected abstract boolean newChainHeadInvalidatesMiningOperation(
      final BlockHeader newChainHeadHeader);

  @Override
  public void changeTargetGasLimit(final Long targetGasLimit) {
    executor.changeTargetGasLimit(targetGasLimit);
  }
}
