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
package org.enterchain.enter.ethereum.mainnet;

import org.enterchain.enter.ethereum.core.TransactionFilter;
import org.enterchain.enter.ethereum.worldstate.WorldStateArchive;

import java.math.BigInteger;
import java.util.Optional;
import java.util.stream.Stream;

public interface ProtocolSchedule {

  ProtocolSpec getByBlockNumber(long number);

  public Stream<Long> streamMilestoneBlocks();

  Optional<BigInteger> getChainId();

  void setTransactionFilter(TransactionFilter transactionFilter);

  void setPublicWorldStateArchiveForPrivacyBlockProcessor(
      WorldStateArchive publicWorldStateArchive);
}
