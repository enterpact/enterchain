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
package org.enterchain.enter.consensus.ibftlegacy.headervalidationrules;

import org.enterchain.enter.consensus.ibftlegacy.IbftLegacyBlockInterface;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.mainnet.DetachedBlockHeaderValidationRule;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VoteValidationRule implements DetachedBlockHeaderValidationRule {

  private static final Logger LOG = LogManager.getLogger();

  /**
   * Responsible for ensuring the nonce is either auth or drop.
   *
   * @param header the block header to validate
   * @param parent the block header corresponding to the parent of the header being validated.
   * @return true if the nonce in the header is a valid validator vote value.
   */
  @Override
  public boolean validate(final BlockHeader header, final BlockHeader parent) {
    final long nonce = header.getNonce();
    if (!IbftLegacyBlockInterface.isValidVoteValue(nonce)) {
      LOG.info("Invalid block header: Nonce value ({}) is neither auth or drop.", nonce);
      return false;
    }
    return true;
  }
}
