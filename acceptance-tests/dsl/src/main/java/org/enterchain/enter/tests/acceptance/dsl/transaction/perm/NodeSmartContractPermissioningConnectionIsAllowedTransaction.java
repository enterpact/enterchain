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
package org.enterchain.enter.tests.acceptance.dsl.transaction.perm;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.enterchain.enter.ethereum.permissioning.NodeSmartContractPermissioningController.checkTransactionResult;

import org.enterchain.enter.crypto.Hash;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.p2p.peers.EnodeURLImpl;
import org.enterchain.enter.ethereum.permissioning.NodeSmartContractPermissioningController;
import org.enterchain.enter.tests.acceptance.dsl.node.Node;
import org.enterchain.enter.tests.acceptance.dsl.node.RunnableNode;
import org.enterchain.enter.tests.acceptance.dsl.transaction.NodeRequests;
import org.enterchain.enter.tests.acceptance.dsl.transaction.Transaction;

import java.io.IOException;

import org.apache.tuweni.bytes.Bytes;
import org.web3j.protocol.core.DefaultBlockParameterName;

public class NodeSmartContractPermissioningConnectionIsAllowedTransaction
    implements Transaction<Boolean> {

  private static final Bytes IS_CONNECTION_ALLOWED_SIGNATURE =
      Hash.keccak256(
              Bytes.of(
                  "connectionAllowed(bytes32,bytes32,bytes16,uint16,bytes32,bytes32,bytes16,uint16)"
                      .getBytes(UTF_8)))
          .slice(0, 4);

  private final Address contractAddress;
  private final Node source;
  private final Node target;

  public NodeSmartContractPermissioningConnectionIsAllowedTransaction(
      final Address contractAddress, final Node source, final Node target) {
    this.contractAddress = contractAddress;
    this.source = source;
    this.target = target;
  }

  @Override
  public Boolean execute(final NodeRequests node) {
    try {
      final String value =
          node.eth().ethCall(payload(), DefaultBlockParameterName.LATEST).send().getValue();
      return checkTransactionResult(Bytes.fromHexString(value));
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  private org.web3j.protocol.core.methods.request.Transaction payload() {
    final String sourceEnodeURL = ((RunnableNode) source).enodeUrl().toASCIIString();
    final String targetEnodeURL = ((RunnableNode) target).enodeUrl().toASCIIString();
    final Bytes payload =
        NodeSmartContractPermissioningController.createPayload(
            IS_CONNECTION_ALLOWED_SIGNATURE,
            EnodeURLImpl.fromString(sourceEnodeURL),
            EnodeURLImpl.fromString(targetEnodeURL));

    return org.web3j.protocol.core.methods.request.Transaction.createFunctionCallTransaction(
        null, null, null, null, contractAddress.toString(), payload.toString());
  }
}
