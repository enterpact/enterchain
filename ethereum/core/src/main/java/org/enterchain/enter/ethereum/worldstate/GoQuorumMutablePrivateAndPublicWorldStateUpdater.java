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
import org.enterchain.enter.ethereum.core.WorldUpdater;

// This class uses a public WorldUpdater and a private WorldUpdater to provide a
// MutableWorldStateUpdater that can read and write from BOTH the private world state and the public
// world state.
public class GoQuorumMutablePrivateAndPublicWorldStateUpdater
    extends GoQuorumMutablePrivateWorldStateUpdater {

  public GoQuorumMutablePrivateAndPublicWorldStateUpdater(
      final WorldUpdater publicWorldUpdater, final WorldUpdater privateWorldUpdater) {
    super(publicWorldUpdater, privateWorldUpdater);
  }

  @Override
  public EvmAccount getAccount(final Address address) {
    final EvmAccount privateAccount = privateWorldUpdater.getAccount(address);
    if (privateAccount != null && !privateAccount.isEmpty()) {
      return privateAccount;
    }
    final EvmAccount publicAccount = publicWorldUpdater.getAccount(address);
    if (publicAccount != null && !publicAccount.isEmpty()) {
      return publicAccount;
    }
    return privateAccount;
  }
}
