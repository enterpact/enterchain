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
package org.enterchain.enter.ethereum.permissioning.account;

import org.enterchain.enter.crypto.SECPSignature;
import org.enterchain.enter.crypto.SignatureAlgorithm;
import org.enterchain.enter.crypto.SignatureAlgorithmFactory;
import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.Transaction;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.permissioning.AccountLocalConfigPermissioningController;
import org.enterchain.enter.ethereum.permissioning.GoQuorumQip714Gate;
import org.enterchain.enter.ethereum.permissioning.LocalPermissioningConfiguration;
import org.enterchain.enter.ethereum.permissioning.PermissioningConfiguration;
import org.enterchain.enter.ethereum.permissioning.SmartContractPermissioningConfiguration;
import org.enterchain.enter.ethereum.permissioning.TransactionSmartContractPermissioningController;
import org.enterchain.enter.ethereum.transaction.TransactionSimulator;
import org.enterchain.enter.plugin.data.TransactionType;
import org.enterchain.enter.plugin.services.MetricsSystem;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

public class AccountPermissioningControllerFactory {

  private static final Logger LOG = LogManager.getLogger();

  public static Optional<AccountPermissioningController> create(
      final PermissioningConfiguration permissioningConfiguration,
      final TransactionSimulator transactionSimulator,
      final MetricsSystem metricsSystem,
      final Blockchain blockchain) {

    if (permissioningConfiguration == null) {
      return Optional.empty();
    }

    final Optional<AccountLocalConfigPermissioningController>
        accountLocalConfigPermissioningController =
            buildLocalConfigPermissioningController(permissioningConfiguration, metricsSystem);

    final Optional<TransactionSmartContractPermissioningController>
        transactionSmartContractPermissioningController =
            buildSmartContractPermissioningController(
                permissioningConfiguration, transactionSimulator, metricsSystem);

    if (accountLocalConfigPermissioningController.isPresent()
        || transactionSmartContractPermissioningController.isPresent()) {

      final Optional<GoQuorumQip714Gate> goQuorumQip714Gate =
          permissioningConfiguration
              .getQuorumPermissioningConfig()
              .flatMap(
                  config -> {
                    if (config.isEnabled()) {
                      return Optional.of(
                          GoQuorumQip714Gate.getInstance(config.getQip714Block(), blockchain));
                    } else {
                      return Optional.empty();
                    }
                  });

      final AccountPermissioningController controller =
          new AccountPermissioningController(
              accountLocalConfigPermissioningController,
              transactionSmartContractPermissioningController,
              goQuorumQip714Gate);

      return Optional.of(controller);
    } else {
      return Optional.empty();
    }
  }

  private static Optional<TransactionSmartContractPermissioningController>
      buildSmartContractPermissioningController(
          final PermissioningConfiguration permissioningConfiguration,
          final TransactionSimulator transactionSimulator,
          final MetricsSystem metricsSystem) {

    if (permissioningConfiguration.getSmartContractConfig().isPresent()) {
      final SmartContractPermissioningConfiguration smartContractPermissioningConfiguration =
          permissioningConfiguration.getSmartContractConfig().get();

      if (smartContractPermissioningConfiguration.isSmartContractAccountAllowlistEnabled()) {
        final Address accountSmartContractAddress =
            smartContractPermissioningConfiguration.getAccountSmartContractAddress();

        Optional<TransactionSmartContractPermissioningController>
            transactionSmartContractPermissioningController =
                Optional.of(
                    new TransactionSmartContractPermissioningController(
                        accountSmartContractAddress, transactionSimulator, metricsSystem));
        validatePermissioningContract(transactionSmartContractPermissioningController.get());

        return transactionSmartContractPermissioningController;
      }
    }

    return Optional.empty();
  }

  private static Optional<AccountLocalConfigPermissioningController>
      buildLocalConfigPermissioningController(
          final PermissioningConfiguration permissioningConfiguration,
          final MetricsSystem metricsSystem) {

    if (permissioningConfiguration.getLocalConfig().isPresent()) {
      final LocalPermissioningConfiguration localPermissioningConfiguration =
          permissioningConfiguration.getLocalConfig().get();

      if (localPermissioningConfiguration.isAccountAllowlistEnabled()) {
        return Optional.of(
            new AccountLocalConfigPermissioningController(
                localPermissioningConfiguration, metricsSystem));
      }
    }

    return Optional.empty();
  }

  private static void validatePermissioningContract(
      final TransactionSmartContractPermissioningController
          transactionSmartContractPermissioningController) {
    try {
      LOG.debug("Validating onchain account permissioning smart contract configuration");

      final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithmFactory.getInstance();

      final SECPSignature FAKE_SIGNATURE =
          signatureAlgorithm.createSignature(
              signatureAlgorithm.getHalfCurveOrder(),
              signatureAlgorithm.getHalfCurveOrder(),
              (byte) 0);

      final Transaction transaction =
          Transaction.builder()
              .type(TransactionType.FRONTIER)
              .sender(Address.ZERO)
              .gasLimit(0)
              .gasPrice(Wei.ZERO)
              .value(Wei.ZERO)
              .payload(Bytes.EMPTY)
              .nonce(0)
              .signature(FAKE_SIGNATURE)
              .build();

      // We don't care about the validation result. All we need it to ensure the check doesn't fail
      transactionSmartContractPermissioningController.isPermitted(transaction);
    } catch (Exception e) {
      final String msg =
          "Error validating onchain account permissioning smart contract configuration";
      LOG.error(msg + ":", e);
      throw new IllegalStateException(msg, e);
    }
  }
}
