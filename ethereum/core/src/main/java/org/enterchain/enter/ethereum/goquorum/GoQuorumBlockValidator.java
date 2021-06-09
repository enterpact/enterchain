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
package org.enterchain.enter.ethereum.goquorum;

import static org.enterchain.enter.ethereum.goquorum.GoQuorumPrivateStateUtil.getPrivateWorldState;

import org.enterchain.enter.ethereum.MainnetBlockValidator;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.chain.BadBlockManager;
import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.GoQuorumPrivacyParameters;
import org.enterchain.enter.ethereum.core.MutableWorldState;
import org.enterchain.enter.ethereum.mainnet.BlockBodyValidator;
import org.enterchain.enter.ethereum.mainnet.BlockHeaderValidator;
import org.enterchain.enter.ethereum.mainnet.BlockProcessor;
import org.enterchain.enter.ethereum.mainnet.BlockProcessor.Result;

import java.util.Optional;

public class GoQuorumBlockValidator extends MainnetBlockValidator {

  private final Optional<GoQuorumPrivacyParameters> goQuorumPrivacyParameters;

  public GoQuorumBlockValidator(
      final BlockHeaderValidator blockHeaderValidator,
      final BlockBodyValidator blockBodyValidator,
      final BlockProcessor blockProcessor,
      final BadBlockManager badBlockManager,
      final Optional<GoQuorumPrivacyParameters> goQuorumPrivacyParameters) {
    super(blockHeaderValidator, blockBodyValidator, blockProcessor, badBlockManager);

    this.goQuorumPrivacyParameters = goQuorumPrivacyParameters;

    if (!(blockProcessor instanceof GoQuorumBlockProcessor)) {
      throw new IllegalStateException(
          "GoQuorumBlockValidator requires an instance of GoQuorumBlockProcessor");
    }
  }

  @Override
  protected Result processBlock(
      final ProtocolContext context, final MutableWorldState worldState, final Block block) {
    final MutableWorldState privateWorldState =
        getPrivateWorldState(goQuorumPrivacyParameters, worldState.rootHash(), block.getHash());

    return ((GoQuorumBlockProcessor) blockProcessor)
        .processBlock(context.getBlockchain(), worldState, privateWorldState, block);
  }
}
