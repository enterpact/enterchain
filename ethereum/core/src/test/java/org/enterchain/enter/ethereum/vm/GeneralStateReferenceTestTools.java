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
package org.enterchain.enter.ethereum.vm;

import static org.assertj.core.api.Assertions.assertThat;

import org.enterchain.enter.ethereum.core.Account;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.core.Log;
import org.enterchain.enter.ethereum.core.MutableWorldState;
import org.enterchain.enter.ethereum.core.Transaction;
import org.enterchain.enter.ethereum.core.WorldState;
import org.enterchain.enter.ethereum.core.WorldUpdater;
import org.enterchain.enter.ethereum.mainnet.MainnetTransactionProcessor;
import org.enterchain.enter.ethereum.mainnet.TransactionValidationParams;
import org.enterchain.enter.ethereum.processing.TransactionProcessingResult;
import org.enterchain.enter.ethereum.referencetests.GeneralStateTestCaseEipSpec;
import org.enterchain.enter.ethereum.referencetests.GeneralStateTestCaseSpec;
import org.enterchain.enter.ethereum.referencetests.ReferenceTestBlockchain;
import org.enterchain.enter.ethereum.referencetests.ReferenceTestProtocolSchedules;
import org.enterchain.enter.ethereum.rlp.RLP;
import org.enterchain.enter.ethereum.worldstate.DefaultMutableWorldState;
import org.enterchain.enter.testutil.JsonTestParameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class GeneralStateReferenceTestTools {
  private static final ReferenceTestProtocolSchedules REFERENCE_TEST_PROTOCOL_SCHEDULES =
      ReferenceTestProtocolSchedules.create();
  private static final List<String> SPECS_PRIOR_TO_DELETING_EMPTY_ACCOUNTS =
      Arrays.asList("Frontier", "Homestead", "EIP150");

  private static MainnetTransactionProcessor transactionProcessor(final String name) {
    return REFERENCE_TEST_PROTOCOL_SCHEDULES
        .getByName(name)
        .getByBlockNumber(0)
        .getTransactionProcessor();
  }

  private static final List<String> EIPS_TO_RUN;

  static {
    final String eips =
        System.getProperty(
            "test.ethereum.state.eips",
            "Frontier,Homestead,EIP150,EIP158,Byzantium,Constantinople,ConstantinopleFix,Istanbul,Berlin");
    EIPS_TO_RUN = Arrays.asList(eips.split(","));
  }

  private static final JsonTestParameters<?, ?> params =
      JsonTestParameters.create(GeneralStateTestCaseSpec.class, GeneralStateTestCaseEipSpec.class)
          .generator(
              (testName, stateSpec, collector) -> {
                final String prefix = testName + "-";
                for (final Map.Entry<String, List<GeneralStateTestCaseEipSpec>> entry :
                    stateSpec.finalStateSpecs().entrySet()) {
                  final String eip = entry.getKey();
                  final boolean runTest = EIPS_TO_RUN.contains(eip);
                  final List<GeneralStateTestCaseEipSpec> eipSpecs = entry.getValue();
                  if (eipSpecs.size() == 1) {
                    collector.add(prefix + eip, eipSpecs.get(0), runTest);
                  } else {
                    for (int i = 0; i < eipSpecs.size(); i++) {
                      collector.add(prefix + eip + '[' + i + ']', eipSpecs.get(i), runTest);
                    }
                  }
                }
              });

  static {
    if (EIPS_TO_RUN.isEmpty()) {
      params.ignoreAll();
    }

    // Known incorrect test.
    params.ignore(
        "RevertPrecompiledTouch(_storage)?-(EIP158|Byzantium|Constantinople|ConstantinopleFix)");

    // Gas integer value is too large to construct a valid transaction.
    params.ignore("OverflowGasRequire");

    // Consumes a huge amount of memory
    params.ignore("static_Call1MB1024Calldepth-\\w");
    params.ignore("ShanghaiLove_.*");

    // Don't do time consuming tests
    params.ignore("CALLBlake2f_MaxRounds.*");
  }

  public static Collection<Object[]> generateTestParametersForConfig(final String[] filePath) {
    return params.generate(filePath);
  }

  public static void executeTest(final GeneralStateTestCaseEipSpec spec) {
    final BlockHeader blockHeader = spec.getBlockHeader();
    final WorldState initialWorldState = spec.getInitialWorldState();
    final Transaction transaction = spec.getTransaction();

    final MutableWorldState worldState = new DefaultMutableWorldState(initialWorldState);
    // Several of the GeneralStateTests check if the transaction could potentially
    // consume more gas than is left for the block it's attempted to be included in.
    // This check is performed within the `BlockImporter` rather than inside the
    // `TransactionProcessor`, so these tests are skipped.
    if (transaction.getGasLimit() > blockHeader.getGasLimit() - blockHeader.getGasUsed()) {
      return;
    }

    final MainnetTransactionProcessor processor = transactionProcessor(spec.getFork());
    final WorldUpdater worldStateUpdater = worldState.updater();
    final ReferenceTestBlockchain blockchain = new ReferenceTestBlockchain(blockHeader.getNumber());
    final TransactionProcessingResult result =
        processor.processTransaction(
            blockchain,
            worldStateUpdater,
            blockHeader,
            transaction,
            blockHeader.getCoinbase(),
            new BlockHashLookup(blockHeader, blockchain),
            false,
            TransactionValidationParams.processingBlock());
    final Account coinbase = worldStateUpdater.getOrCreate(spec.getBlockHeader().getCoinbase());
    if (coinbase != null && coinbase.isEmpty() && shouldClearEmptyAccounts(spec.getFork())) {
      worldStateUpdater.deleteAccount(coinbase.getAddress());
    }
    worldStateUpdater.commit();

    // Check the world state root hash.
    final Hash expectedRootHash = spec.getExpectedRootHash();
    assertThat(worldState.rootHash())
        .withFailMessage("Unexpected world state root hash; computed state: %s", worldState)
        .isEqualTo(expectedRootHash);

    // Check the logs.
    final Hash expectedLogsHash = spec.getExpectedLogsHash();
    final List<Log> logs = result.getLogs();
    assertThat(Hash.hash(RLP.encode(out -> out.writeList(logs, Log::writeTo))))
        .withFailMessage("Unmatched logs hash. Generated logs: %s", logs)
        .isEqualTo(expectedLogsHash);
  }

  private static boolean shouldClearEmptyAccounts(final String eip) {
    return !SPECS_PRIOR_TO_DELETING_EMPTY_ACCOUNTS.contains(eip);
  }
}
