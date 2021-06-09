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
package org.enterchain.enter.ethereum.api.graphql;

import org.enterchain.enter.ethereum.api.handlers.IsAliveHandler;
import org.enterchain.enter.ethereum.api.query.BlockchainQueries;
import org.enterchain.enter.ethereum.blockcreation.MiningCoordinator;
import org.enterchain.enter.ethereum.core.Synchronizer;
import org.enterchain.enter.ethereum.eth.transactions.TransactionPool;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;

public interface GraphQLDataFetcherContext {

  TransactionPool getTransactionPool();

  BlockchainQueries getBlockchainQueries();

  MiningCoordinator getMiningCoordinator();

  Synchronizer getSynchronizer();

  ProtocolSchedule getProtocolSchedule();

  default IsAliveHandler getIsAliveHandler() {
    return new IsAliveHandler(true);
  }
}
