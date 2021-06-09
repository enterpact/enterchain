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
package org.enterchain.enter.ethereum.vm.operations;

import static java.util.Collections.emptyList;

import org.enterchain.enter.ethereum.chain.MutableBlockchain;
import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.BlockBody;
import org.enterchain.enter.ethereum.core.BlockHeaderTestFixture;
import org.enterchain.enter.ethereum.core.Difficulty;
import org.enterchain.enter.ethereum.core.ExecutionContextTestFixture;
import org.enterchain.enter.ethereum.core.MessageFrameTestFixture;
import org.enterchain.enter.ethereum.vm.MessageFrame;
import org.enterchain.enter.metrics.noop.NoOpMetricsSystem;
import org.enterchain.enter.plugin.services.storage.KeyValueStorage;
import org.enterchain.enter.plugin.services.storage.rocksdb.RocksDBMetricsFactory;
import org.enterchain.enter.plugin.services.storage.rocksdb.configuration.RocksDBConfigurationBuilder;
import org.enterchain.enter.plugin.services.storage.rocksdb.unsegmented.RocksDBKeyValueStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;

public class OperationBenchmarkHelper {

  private final Path storageDirectory;
  private final KeyValueStorage keyValueStorage;
  private final MessageFrame messageFrame;

  private OperationBenchmarkHelper(
      final Path storageDirectory,
      final KeyValueStorage keyValueStorage,
      final MessageFrame messageFrame) {
    this.storageDirectory = storageDirectory;
    this.keyValueStorage = keyValueStorage;
    this.messageFrame = messageFrame;
  }

  public static OperationBenchmarkHelper create() throws IOException {
    final Path storageDirectory = Files.createTempDirectory("benchmark");
    final KeyValueStorage keyValueStorage =
        new RocksDBKeyValueStorage(
            new RocksDBConfigurationBuilder().databaseDir(storageDirectory).build(),
            new NoOpMetricsSystem(),
            RocksDBMetricsFactory.PUBLIC_ROCKS_DB_METRICS);

    final ExecutionContextTestFixture executionContext =
        ExecutionContextTestFixture.builder().keyValueStorage(keyValueStorage).build();
    final MutableBlockchain blockchain = executionContext.getBlockchain();

    for (int i = 1; i < 256; i++) {
      blockchain.appendBlock(
          new Block(
              new BlockHeaderTestFixture()
                  .parentHash(blockchain.getChainHeadHash())
                  .number(i)
                  .difficulty(Difficulty.ONE)
                  .buildHeader(),
              new BlockBody(emptyList(), emptyList())),
          emptyList());
    }
    final MessageFrame messageFrame =
        new MessageFrameTestFixture()
            .executionContextTestFixture(executionContext)
            .blockHeader(
                new BlockHeaderTestFixture()
                    .parentHash(blockchain.getChainHeadHash())
                    .number(blockchain.getChainHeadBlockNumber() + 1)
                    .difficulty(Difficulty.ONE)
                    .buildHeader())
            .build();
    return new OperationBenchmarkHelper(storageDirectory, keyValueStorage, messageFrame);
  }

  public MessageFrame createMessageFrame() {
    return createMessageFrameBuilder().build();
  }

  public MessageFrame.Builder createMessageFrameBuilder() {
    return MessageFrame.builder()
        .type(MessageFrame.Type.MESSAGE_CALL)
        .messageFrameStack(messageFrame.getMessageFrameStack())
        .blockchain(messageFrame.getBlockchain())
        .worldState(messageFrame.getWorldState())
        .initialGas(messageFrame.getRemainingGas())
        .address(messageFrame.getContractAddress())
        .originator(messageFrame.getOriginatorAddress())
        .contract(messageFrame.getRecipientAddress())
        .gasPrice(messageFrame.getGasPrice())
        .inputData(messageFrame.getInputData())
        .sender(messageFrame.getSenderAddress())
        .value(messageFrame.getValue())
        .apparentValue(messageFrame.getApparentValue())
        .code(messageFrame.getCode())
        .blockHeader(messageFrame.getBlockHeader())
        .depth(messageFrame.getMessageStackDepth())
        .isStatic(messageFrame.isStatic())
        .completer(messageFrame -> {})
        .miningBeneficiary(messageFrame.getMiningBeneficiary())
        .maxStackSize(messageFrame.getMaxStackSize())
        .blockHashLookup(messageFrame.getBlockHashLookup());
  }

  public void cleanUp() throws IOException {
    keyValueStorage.close();
    MoreFiles.deleteRecursively(storageDirectory, RecursiveDeleteOption.ALLOW_INSECURE);
  }
}
