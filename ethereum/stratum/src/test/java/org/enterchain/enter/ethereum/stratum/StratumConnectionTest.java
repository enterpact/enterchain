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
package org.enterchain.enter.ethereum.stratum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.enterchain.enter.ethereum.blockcreation.PoWMiningCoordinator;
import org.enterchain.enter.ethereum.mainnet.EpochCalculator;
import org.enterchain.enter.ethereum.mainnet.PoWSolverInputs;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.vertx.core.buffer.Buffer;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class StratumConnectionTest {

  @Mock PoWMiningCoordinator miningCoordinator;

  @Before
  public void setup() {
    miningCoordinator = Mockito.mock(PoWMiningCoordinator.class);
    when(miningCoordinator.getEpochCalculator())
        .thenReturn(new EpochCalculator.DefaultEpochCalculator());
  }

  @Test
  public void testNoSuitableProtocol() {
    AtomicBoolean called = new AtomicBoolean(false);
    StratumConnection conn =
        new StratumConnection(new StratumProtocol[] {}, () -> called.set(true), bytes -> {});
    conn.handleBuffer(Buffer.buffer("{}\n"));
    assertThat(called.get()).isTrue();
  }

  @Test
  public void testStratum1WithoutMatches() {
    AtomicBoolean called = new AtomicBoolean(false);
    StratumConnection conn =
        new StratumConnection(
            new StratumProtocol[] {new Stratum1Protocol("", miningCoordinator)},
            () -> called.set(true),
            bytes -> {});
    conn.handleBuffer(Buffer.buffer("{}\n"));
    assertThat(called.get()).isTrue();
  }

  @Test
  public void testStratum1Matches() {

    AtomicBoolean called = new AtomicBoolean(false);

    AtomicReference<String> message = new AtomicReference<>();

    StratumConnection conn =
        new StratumConnection(
            new StratumProtocol[] {
              new Stratum1Protocol("", miningCoordinator, () -> "abcd", () -> "abcd")
            },
            () -> called.set(true),
            message::set);
    conn.handleBuffer(
        Buffer.buffer(
            "{"
                + "  \"id\": 23,"
                + "  \"method\": \"mining.subscribe\", "
                + "  \"params\": [ "
                + "    \"MinerName/1.0.0\", \"EthereumStratum/1.0.0\" "
                + "  ]"
                + "}\n"));
    assertThat(called.get()).isFalse();

    assertThat(message.get())
        .isEqualTo(
            "{\"jsonrpc\":\"2.0\",\"id\":23,\"result\":[[\"mining.notify\",\"abcd\",\"EthereumStratum/1.0.0\"],\"\"]}\n");
  }

  @Test
  public void testStratum1SendWork() {

    AtomicBoolean called = new AtomicBoolean(false);

    AtomicReference<String> message = new AtomicReference<>();

    Stratum1Protocol protocol =
        new Stratum1Protocol("", miningCoordinator, () -> "abcd", () -> "abcd");

    StratumConnection conn =
        new StratumConnection(
            new StratumProtocol[] {protocol}, () -> called.set(true), message::set);
    conn.handleBuffer(
        Buffer.buffer(
            "{"
                + "  \"id\": 23,"
                + "  \"method\": \"mining.subscribe\", "
                + "  \"params\": [ "
                + "    \"MinerName/1.0.0\", \"EthereumStratum/1.0.0\" "
                + "  ]"
                + "}\n"));
    conn.handleBuffer(
        Buffer.buffer(
            "{"
                + "  \"id\": null,"
                + "  \"method\": \"mining.authorize\", "
                + "  \"params\": [ "
                + "    \"someusername\", \"password\" "
                + "  ]"
                + "}\n"));
    assertThat(called.get()).isFalse();
    // now send work without waiting.
    protocol.setCurrentWorkTask(
        new PoWSolverInputs(UInt256.valueOf(3), Bytes.fromHexString("deadbeef"), 42));

    assertThat(message.get())
        .isEqualTo(
            "{\"jsonrpc\":\"2.0\",\"method\":\"mining.notify\",\"params\":[\"abcd\",\"0xdeadbeef\",\"0x0000000000000000000000000000000000000000000000000000000000000000\",\"0x0000000000000000000000000000000000000000000000000000000000000003\",true],\"id\":null}\n");
  }

  @Test
  public void testStratum1SubmitHashrate() {

    AtomicBoolean called = new AtomicBoolean(false);

    AtomicReference<String> message = new AtomicReference<>();

    Stratum1Protocol protocol =
        new Stratum1Protocol("", miningCoordinator, () -> "abcd", () -> "abcd");

    Mockito.when(miningCoordinator.submitHashRate("0x02", 3L)).thenReturn(true);

    StratumConnection conn =
        new StratumConnection(
            new StratumProtocol[] {protocol}, () -> called.set(true), message::set);
    conn.handleBuffer(
        Buffer.buffer(
            "{"
                + "  \"id\": 23,"
                + "  \"method\": \"mining.subscribe\", "
                + "  \"params\": [ "
                + "    \"MinerName/1.0.0\", \"EthereumStratum/1.0.0\" "
                + "  ]"
                + "}\n"));
    conn.handleBuffer(
        Buffer.buffer(
            "{"
                + "  \"id\": 23,"
                + "  \"method\": \"eth_submitHashrate\", "
                + "  \"params\": [ "
                + "    \"0x03\",\"0x02\" "
                + "  ]"
                + "}\n"));
    assertThat(called.get()).isFalse();

    assertThat(message.get()).isEqualTo("{\"jsonrpc\":\"2.0\",\"id\":23,\"result\":true}\n");
  }
}
