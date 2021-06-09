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
public interface NodeConnectionPermissioningProvider {
  /**
   * Can be used to intercept the initial connection to a peer. Note that once a connection is
   * established it's bidirectional.
   *
   * @param sourceEnode the originator's enode
   * @param destinationEnode the enode you are attempting to connect to
   * @return if the connection is permitted
   */
  boolean isConnectionPermitted(final EnodeURL sourceEnode, final EnodeURL destinationEnode);
}
