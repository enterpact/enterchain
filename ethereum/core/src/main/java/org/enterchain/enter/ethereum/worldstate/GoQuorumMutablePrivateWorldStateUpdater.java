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
package org.enterchain.enter.ethereum.worldstate;

import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.EvmAccount;
import org.enterchain.enter.ethereum.core.UpdateTrackingAccount;
import org.enterchain.enter.ethereum.core.WorldUpdater;
import org.enterchain.enter.ethereum.vm.MessageFrame;

// This class uses a public WorldUpdater and a private WorldUpdater to provide a
// MutableWorldStateUpdater that can read and write from the private world state and can read from
// the public world state, but cannot write to it.
public class GoQuorumMutablePrivateWorldStateUpdater
    extends DefaultMutablePrivateWorldStateUpdater {

  public GoQuorumMutablePrivateWorldStateUpdater(
      final WorldUpdater publicWorldUpdater, final WorldUpdater privateWorldUpdater) {
    super(publicWorldUpdater, privateWorldUpdater);
  }

  @Override
  public EvmAccount getOrCreateSenderAccount(final Address address) {
    return new UpdateTrackingAccount<>(publicWorldUpdater.getOrCreate(address));
  }

  @Override
  public EvmAccount getSenderAccount(final MessageFrame frame) {
    final Address senderAddress = frame.getSenderAddress();
    if (senderAddress.equals(frame.getOriginatorAddress())) {
      return new UpdateTrackingAccount<>(publicWorldUpdater.getOrCreate(senderAddress));
    } else {
      return getAccount(senderAddress);
    }
  }
}
