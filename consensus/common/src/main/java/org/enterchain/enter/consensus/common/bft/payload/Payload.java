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
package org.enterchain.enter.consensus.common.bft.payload;

import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.rlp.BytesValueRLPOutput;
import org.enterchain.enter.ethereum.rlp.RLPInput;
import org.enterchain.enter.ethereum.rlp.RLPOutput;

import org.apache.tuweni.bytes.Bytes;

public interface Payload extends RoundSpecific {

  void writeTo(final RLPOutput rlpOutput);

  default Bytes encoded() {
    BytesValueRLPOutput rlpOutput = new BytesValueRLPOutput();
    writeTo(rlpOutput);

    return rlpOutput.encoded();
  }

  int getMessageType();

  static Hash readDigest(final RLPInput messageData) {
    return Hash.wrap(messageData.readBytes32());
  }

  Hash hashForSignature();
}
