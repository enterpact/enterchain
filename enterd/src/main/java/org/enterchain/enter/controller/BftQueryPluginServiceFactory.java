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
package org.enterchain.enter.controller;

import org.enterchain.enter.consensus.common.bft.BftBlockInterface;
import org.enterchain.enter.consensus.common.bft.BftExtraDataCodec;
import org.enterchain.enter.consensus.common.bft.queries.BftQueryServiceImpl;
import org.enterchain.enter.crypto.NodeKey;
import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.plugin.services.metrics.PoAMetricsService;
import org.enterchain.enter.plugin.services.query.BftQueryService;
import org.enterchain.enter.plugin.services.query.PoaQueryService;
import org.enterchain.enter.services.BesuPluginContextImpl;

public class BftQueryPluginServiceFactory implements PluginServiceFactory {

  private final Blockchain blockchain;
  private final BftExtraDataCodec bftExtraDataCodec;
  private final NodeKey nodeKey;
  private final String consensusMechanismName;

  public BftQueryPluginServiceFactory(
      final Blockchain blockchain,
      final BftExtraDataCodec bftExtraDataCodec,
      final NodeKey nodeKey,
      final String consensusMechanismName) {
    this.blockchain = blockchain;
    this.bftExtraDataCodec = bftExtraDataCodec;
    this.nodeKey = nodeKey;
    this.consensusMechanismName = consensusMechanismName;
  }

  @Override
  public void appendPluginServices(final BesuPluginContextImpl besuContext) {
    final BftBlockInterface blockInterface = new BftBlockInterface(bftExtraDataCodec);

    final BftQueryServiceImpl service =
        new BftQueryServiceImpl(blockInterface, blockchain, nodeKey, consensusMechanismName);
    besuContext.addService(BftQueryService.class, service);
    besuContext.addService(PoaQueryService.class, service);
    besuContext.addService(PoAMetricsService.class, service);
  }
}
