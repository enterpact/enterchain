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
package org.enterchain.enter.ethereum.api.jsonrpc.internal.processor;

import static java.util.function.Predicate.isEqual;

import org.enterchain.enter.ethereum.api.jsonrpc.internal.parameters.TransactionTraceParams;
import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.core.AbstractWorldUpdater;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.core.Transaction;
import org.enterchain.enter.ethereum.core.WorldUpdater;
import org.enterchain.enter.ethereum.debug.TraceOptions;
import org.enterchain.enter.ethereum.mainnet.ImmutableTransactionValidationParams;
import org.enterchain.enter.ethereum.mainnet.MainnetTransactionProcessor;
import org.enterchain.enter.ethereum.processing.TransactionProcessingResult;
import org.enterchain.enter.ethereum.vm.BlockHashLookup;
import org.enterchain.enter.ethereum.vm.DebugOperationTracer;
import org.enterchain.enter.ethereum.vm.OperationTracer;
import org.enterchain.enter.ethereum.vm.StandardJsonTracer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;

/** Used to produce debug traces of transactions */
public class TransactionTracer {

  public static final String TRACE_PATH = "traces";

  private final BlockReplay blockReplay;

  public TransactionTracer(final BlockReplay blockReplay) {
    this.blockReplay = blockReplay;
  }

  public Optional<TransactionTrace> traceTransaction(
      final Hash blockHash, final Hash transactionHash, final DebugOperationTracer tracer) {
    return blockReplay.beforeTransactionInBlock(
        blockHash,
        transactionHash,
        (transaction, header, blockchain, worldState, transactionProcessor) -> {
          final TransactionProcessingResult result =
              processTransaction(
                  header,
                  blockchain,
                  worldState.updater(),
                  transaction,
                  transactionProcessor,
                  tracer);
          return new TransactionTrace(transaction, result, tracer.getTraceFrames());
        });
  }

  public List<String> traceTransactionToFile(
      final Hash blockHash,
      final Optional<TransactionTraceParams> transactionTraceParams,
      final Path traceDir) {

    final Optional<Hash> selectedHash =
        transactionTraceParams
            .map(TransactionTraceParams::getTransactionHash)
            .map(Hash::fromHexString);
    final boolean showMemory =
        transactionTraceParams
            .map(TransactionTraceParams::traceOptions)
            .map(TraceOptions::isMemoryEnabled)
            .orElse(true);

    if (!Files.isDirectory(traceDir) && !traceDir.toFile().mkdirs()) {
      throw new RuntimeException(
          String.format("Trace directory '%s' does not exist and could not be made.", traceDir));
    }

    return blockReplay
        .performActionWithBlock(
            blockHash,
            (body, header, blockchain, worldState, transactionProcessor) -> {
              WorldUpdater stackedUpdater = worldState.updater().updater();
              final List<String> traces = new ArrayList<>();
              for (int i = 0; i < body.getTransactions().size(); i++) {
                ((AbstractWorldUpdater.StackedUpdater) stackedUpdater).markTransactionBoundary();
                final Transaction transaction = body.getTransactions().get(i);
                if (selectedHash.isEmpty()
                    || selectedHash.filter(isEqual(transaction.getHash())).isPresent()) {
                  final File traceFile = generateTraceFile(traceDir, blockHash, i, transaction);
                  try (PrintStream out = new PrintStream(new FileOutputStream(traceFile))) {
                    final Stopwatch timer = Stopwatch.createStarted();
                    final TransactionProcessingResult result =
                        processTransaction(
                            header,
                            blockchain,
                            stackedUpdater,
                            transaction,
                            transactionProcessor,
                            new StandardJsonTracer(out, showMemory));
                    out.println(
                        StandardJsonTracer.summaryTrace(
                            transaction, timer.stop().elapsed(TimeUnit.NANOSECONDS), result));
                    traces.add(traceFile.getPath());
                  } catch (FileNotFoundException e) {
                    throw new RuntimeException(
                        "Unable to create transaction trace : " + e.getMessage());
                  }
                } else {
                  processTransaction(
                      header,
                      blockchain,
                      stackedUpdater,
                      transaction,
                      transactionProcessor,
                      OperationTracer.NO_TRACING);
                }
              }
              return Optional.of(traces);
            })
        .orElse(new ArrayList<>());
  }

  private File generateTraceFile(
      final Path traceDir,
      final Hash blockHash,
      final int indexTransaction,
      final Transaction transaction) {
    return traceDir
        .resolve(
            String.format(
                "block_%.10s-%d-%.10s-%s",
                blockHash.toHexString(),
                indexTransaction,
                transaction.getHash().toHexString(),
                System.currentTimeMillis()))
        .toFile();
  }

  private TransactionProcessingResult processTransaction(
      final BlockHeader header,
      final Blockchain blockchain,
      final WorldUpdater worldUpdater,
      final Transaction transaction,
      final MainnetTransactionProcessor transactionProcessor,
      final OperationTracer tracer) {
    return transactionProcessor.processTransaction(
        blockchain,
        worldUpdater,
        header,
        transaction,
        header.getCoinbase(),
        tracer,
        new BlockHashLookup(header, blockchain),
        false,
        ImmutableTransactionValidationParams.builder().isAllowFutureNonce(true).build());
  }
}
