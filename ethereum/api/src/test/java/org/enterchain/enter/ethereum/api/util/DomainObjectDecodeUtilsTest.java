/*
 *
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
 *
 */
package org.enterchain.enter.ethereum.api.util;

import org.enterchain.enter.crypto.SECPSignature;
import org.enterchain.enter.crypto.SignatureAlgorithmFactory;
import org.enterchain.enter.ethereum.core.AccessListEntry;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.BlockDataGenerator;
import org.enterchain.enter.ethereum.core.Transaction;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.core.encoding.TransactionEncoder;
import org.enterchain.enter.ethereum.rlp.BytesValueRLPOutput;

import java.math.BigInteger;
import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class DomainObjectDecodeUtilsTest {

  static final BlockDataGenerator gen = new BlockDataGenerator();
  private static final SECPSignature signature =
      SignatureAlgorithmFactory.getInstance()
          .createSignature(BigInteger.ONE, BigInteger.TEN, (byte) 1);
  private static final Address sender =
      Address.fromHexString("0x0000000000000000000000000000000000000003");

  private static final Transaction accessListTxn =
      Transaction.builder()
          .chainId(BigInteger.valueOf(2018))
          .accessList(List.of(new AccessListEntry(gen.address(), List.of(gen.bytes32()))))
          .nonce(1)
          .gasPrice(Wei.of(12))
          .gasLimit(43)
          .payload(Bytes.EMPTY)
          .value(Wei.ZERO)
          .signature(signature)
          .sender(sender)
          .guessType()
          .build();

  @Test
  public void testAccessListRLPSerDes() {
    final BytesValueRLPOutput encoded = new BytesValueRLPOutput();
    TransactionEncoder.encodeForWire(accessListTxn, encoded);
    Transaction decoded =
        DomainObjectDecodeUtils.decodeRawTransaction(encoded.encoded().toHexString());
    Assertions.assertThat(decoded.getAccessList().isPresent()).isTrue();
    Assertions.assertThat(decoded.getAccessList().map(List::size).get()).isEqualTo(1);
  }

  @Test
  public void testAccessList2718OpaqueSerDes() {
    final Bytes encoded = TransactionEncoder.encodeOpaqueBytes(accessListTxn);
    Transaction decoded = DomainObjectDecodeUtils.decodeRawTransaction(encoded.toString());
    Assertions.assertThat(decoded.getAccessList().isPresent()).isTrue();
    Assertions.assertThat(decoded.getAccessList().map(List::size).get()).isEqualTo(1);
  }
}
