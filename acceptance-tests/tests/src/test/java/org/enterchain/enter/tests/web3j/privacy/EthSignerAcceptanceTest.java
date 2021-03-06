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
package org.enterchain.enter.tests.web3j.privacy;

import org.enterchain.enter.tests.acceptance.dsl.ethsigner.EthSignerClient;
import org.enterchain.enter.tests.acceptance.dsl.ethsigner.testutil.EthSignerTestHarness;
import org.enterchain.enter.tests.acceptance.dsl.ethsigner.testutil.EthSignerTestHarnessFactory;
import org.enterchain.enter.tests.acceptance.dsl.privacy.PrivacyAcceptanceTestBase;
import org.enterchain.enter.tests.acceptance.dsl.privacy.PrivacyNode;
import org.enterchain.enter.tests.web3j.generated.EventEmitter;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.web3j.protocol.besu.response.privacy.PrivacyGroup;
import org.web3j.protocol.besu.response.privacy.PrivateTransactionReceipt;
import org.web3j.utils.Base64String;

public class EthSignerAcceptanceTest extends PrivacyAcceptanceTestBase {

  private PrivacyNode minerNode;
  private EthSignerTestHarness ethSigner;

  private EthSignerClient ethSignerClient;

  @Before
  public void setUp() throws Exception {
    minerNode =
        privacyBesu.createPrivateTransactionEnabledMinerNode(
            "miner-node", privacyAccountResolver.resolve(0));
    privacyCluster.start(minerNode);

    ethSigner =
        EthSignerTestHarnessFactory.create(
            privacy.newFolder().toPath(),
            "ethSignerKey--fe3b557e8fb62b89f4916b721be55ceb828dbd73.json",
            minerNode.getBesu().getJsonRpcSocketPort().orElseThrow(),
            1337);

    ethSignerClient = new EthSignerClient(ethSigner.getHttpListeningUrl());
  }

  @Test
  public void privateSmartContractMustDeploy() throws IOException {
    final String transactionHash =
        ethSignerClient.eeaSendTransaction(
            null,
            BigInteger.valueOf(23176),
            BigInteger.valueOf(1000),
            EventEmitter.BINARY,
            BigInteger.valueOf(0),
            minerNode.getEnclaveKey(),
            Collections.emptyList(),
            "restricted");

    final PrivateTransactionReceipt receipt =
        minerNode.execute(privacyTransactions.getPrivateTransactionReceipt(transactionHash));

    minerNode.verify(
        privateTransactionVerifier.validPrivateTransactionReceipt(transactionHash, receipt));
  }

  // requires ethsigner jar > 0.3.0
  // https://cloudsmith.io/~consensys/repos/ethsigner/packages/
  @Test
  @Ignore
  public void privateSmartContractMustDeployNoNonce() throws IOException {
    final String transactionHash =
        ethSignerClient.eeaSendTransaction(
            null,
            BigInteger.valueOf(23176),
            BigInteger.valueOf(1000),
            EventEmitter.BINARY,
            minerNode.getEnclaveKey(),
            Collections.emptyList(),
            "restricted");

    final PrivateTransactionReceipt receipt =
        minerNode.execute(privacyTransactions.getPrivateTransactionReceipt(transactionHash));

    minerNode.verify(
        privateTransactionVerifier.validPrivateTransactionReceipt(transactionHash, receipt));
  }

  @Test
  public void privateSmartContractMustDeployWithPrivacyGroup() throws IOException {
    final String privacyGroupId =
        minerNode.execute(privacyTransactions.createPrivacyGroup(null, null, minerNode));

    minerNode.verify(
        privateTransactionVerifier.validPrivacyGroupCreated(
            new PrivacyGroup(
                privacyGroupId,
                PrivacyGroup.Type.PANTHEON,
                "",
                "",
                Base64String.wrapList(minerNode.getEnclaveKey()))));

    final String transactionHash =
        ethSignerClient.eeaSendTransaction(
            null,
            BigInteger.valueOf(23176),
            BigInteger.valueOf(1000),
            EventEmitter.BINARY,
            BigInteger.valueOf(0),
            minerNode.getEnclaveKey(),
            privacyGroupId,
            "restricted");

    final PrivateTransactionReceipt receipt =
        minerNode.execute(privacyTransactions.getPrivateTransactionReceipt(transactionHash));

    minerNode.verify(
        privateTransactionVerifier.validPrivateTransactionReceipt(transactionHash, receipt));
  }

  @Test
  public void privateSmartContractMustDeployWithPrivacyGroupNoNonce() throws IOException {
    final String privacyGroupId =
        minerNode.execute(privacyTransactions.createPrivacyGroup(null, null, minerNode));

    minerNode.verify(
        privateTransactionVerifier.validPrivacyGroupCreated(
            new PrivacyGroup(
                privacyGroupId,
                PrivacyGroup.Type.PANTHEON,
                "",
                "",
                Base64String.wrapList(minerNode.getEnclaveKey()))));

    final String transactionHash =
        ethSignerClient.eeaSendTransaction(
            null,
            BigInteger.valueOf(23176),
            BigInteger.valueOf(1000),
            EventEmitter.BINARY,
            minerNode.getEnclaveKey(),
            privacyGroupId,
            "restricted");

    final PrivateTransactionReceipt receipt =
        minerNode.execute(privacyTransactions.getPrivateTransactionReceipt(transactionHash));

    minerNode.verify(
        privateTransactionVerifier.validPrivateTransactionReceipt(transactionHash, receipt));
  }
}
