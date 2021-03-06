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
package org.enterchain.enter.ethereum;

import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.MutableWorldState;
import org.enterchain.enter.ethereum.core.TransactionReceipt;
import org.enterchain.enter.ethereum.mainnet.HeaderValidationMode;

import java.util.List;
import java.util.Optional;

public interface BlockValidator {

  class BlockProcessingOutputs {
    public final MutableWorldState worldState;
    public final List<TransactionReceipt> receipts;

    public BlockProcessingOutputs(
        final MutableWorldState worldState, final List<TransactionReceipt> receipts) {
      this.worldState = worldState;
      this.receipts = receipts;
    }
  }

  Optional<BlockProcessingOutputs> validateAndProcessBlock(
      final ProtocolContext context,
      final Block block,
      final HeaderValidationMode headerValidationMode,
      final HeaderValidationMode ommerValidationMode);

  boolean fastBlockValidation(
      final ProtocolContext context,
      final Block block,
      final List<TransactionReceipt> receipts,
      final HeaderValidationMode headerValidationMode,
      final HeaderValidationMode ommerValidationMode);
}
