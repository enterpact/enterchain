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
package org.enterchain.enter.ethereum.api.util;

import org.enterchain.enter.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcRequestException;
import org.enterchain.enter.ethereum.core.Transaction;
import org.enterchain.enter.ethereum.core.encoding.TransactionDecoder;
import org.enterchain.enter.ethereum.rlp.RLPException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;

public class DomainObjectDecodeUtils {
  private static final Logger LOG = LogManager.getLogger();

  public static Transaction decodeRawTransaction(final String rawTransaction)
      throws InvalidJsonRpcRequestException {
    try {
      Bytes txnBytes = Bytes.fromHexString(rawTransaction);
      return TransactionDecoder.decodeOpaqueBytes(txnBytes);
    } catch (final IllegalArgumentException | RLPException e) {
      LOG.debug(e);
      throw new InvalidJsonRpcRequestException("Invalid raw transaction hex", e);
    }
  }
}
