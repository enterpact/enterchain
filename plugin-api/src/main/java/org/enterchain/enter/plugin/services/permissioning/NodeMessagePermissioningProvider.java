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

package org.enterchain.enter.plugin.services.permissioning;

import org.enterchain.enter.plugin.data.EnodeURL;

@FunctionalInterface
public interface NodeMessagePermissioningProvider {
  /**
   * Can be used to intercept messages before they are sent from besu.
   *
   * @param destinationEnode the enode you are about to send to
   * @param code devp2p code for the message
   * @return if we can send the message to the peer
   */
  boolean isMessagePermitted(final EnodeURL destinationEnode, final int code);
}
