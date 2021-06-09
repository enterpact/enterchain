/*
 * Copyright 2020 ConsenSys AG.
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
package org.enterchain.enter.consensus.qbft.validation;

import org.enterchain.enter.consensus.common.bft.ConsensusRoundIdentifier;
import org.enterchain.enter.consensus.common.bft.payload.SignedData;
import org.enterchain.enter.consensus.qbft.messagewrappers.Prepare;
import org.enterchain.enter.consensus.qbft.payload.PreparePayload;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.Hash;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PrepareValidator {

  private static final String ERROR_PREFIX = "Invalid Prepare Message";

  private static final Logger LOG = LogManager.getLogger();

  private final Collection<Address> validators;
  private final ConsensusRoundIdentifier targetRound;
  private final Hash expectedDigest;

  public PrepareValidator(
      final Collection<Address> validators,
      final ConsensusRoundIdentifier targetRound,
      final Hash expectedDigest) {
    this.validators = validators;
    this.targetRound = targetRound;
    this.expectedDigest = expectedDigest;
  }

  public boolean validate(final Prepare msg) {
    return validate(msg.getSignedPayload());
  }

  public boolean validate(final SignedData<PreparePayload> signedPayload) {
    if (!validators.contains(signedPayload.getAuthor())) {
      LOG.info("{}: did not originate from a recognized validator.", ERROR_PREFIX);
      return false;
    }

    final PreparePayload payload = signedPayload.getPayload();

    if (!payload.getRoundIdentifier().equals(targetRound)) {
      LOG.info("{}: did not target expected round/height", ERROR_PREFIX);
      return false;
    }

    if (!payload.getDigest().equals(expectedDigest)) {
      LOG.info("{}: did not contain expected digest", ERROR_PREFIX);
      return false;
    }

    return true;
  }
}
