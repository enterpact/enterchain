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
package org.enterchain.enter.ethereum.api.jsonrpc.internal.methods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.enterchain.enter.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcError;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.results.proof.GetProofResult;
import org.enterchain.enter.ethereum.api.query.BlockchainQueries;
import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.chain.ChainHead;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.core.MutableWorldState;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.proof.WorldStateProof;
import org.enterchain.enter.ethereum.worldstate.StateTrieAccountValue;
import org.enterchain.enter.ethereum.worldstate.WorldStateArchive;

import java.util.Collections;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EthGetProofTest {
  @Mock private Blockchain blockchain;
  @Mock private BlockchainQueries blockchainQueries;
  @Mock private ChainHead chainHead;

  private EthGetProof method;
  private final String JSON_RPC_VERSION = "2.0";
  private final String ETH_METHOD = "eth_getProof";

  private final Address address =
      Address.fromHexString("0x1234567890123456789012345678901234567890");
  private final UInt256 storageKey =
      UInt256.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000001");
  private final long blockNumber = 1;

  @Before
  public void setUp() {
    method = new EthGetProof(blockchainQueries);
  }

  @Test
  public void returnsCorrectMethodName() {
    assertThat(method.getName()).isEqualTo(ETH_METHOD);
  }

  @Test
  public void errorWhenNoAddressAccountSupplied() {
    final JsonRpcRequestContext request = requestWithParams(null, null, "latest");
    when(blockchainQueries.getBlockchain()).thenReturn(blockchain);
    when(blockchainQueries.getBlockchain().getChainHead()).thenReturn(chainHead);
    when(blockchainQueries.getBlockchain().getChainHead().getHash()).thenReturn(Hash.ZERO);

    Assertions.assertThatThrownBy(() -> method.response(request))
        .isInstanceOf(InvalidJsonRpcParameters.class)
        .hasMessageContaining("Missing required json rpc parameter at index 0");
  }

  @Test
  public void errorWhenNoStorageKeysSupplied() {
    final JsonRpcRequestContext request = requestWithParams(address.toString(), null, "latest");
    when(blockchainQueries.getBlockchain()).thenReturn(blockchain);
    when(blockchainQueries.getBlockchain().getChainHead()).thenReturn(chainHead);
    when(blockchainQueries.getBlockchain().getChainHead().getHash()).thenReturn(Hash.ZERO);

    Assertions.assertThatThrownBy(() -> method.response(request))
        .isInstanceOf(InvalidJsonRpcParameters.class)
        .hasMessageContaining("Missing required json rpc parameter at index 1");
  }

  @Test
  public void errorWhenNoBlockNumberSupplied() {
    final JsonRpcRequestContext request = requestWithParams(address.toString(), new String[] {});

    Assertions.assertThatThrownBy(() -> method.response(request))
        .isInstanceOf(InvalidJsonRpcParameters.class)
        .hasMessageContaining("Missing required json rpc parameter at index 2");
  }

  @Test
  public void errorWhenAccountNotFound() {

    generateWorldState();

    final JsonRpcErrorResponse expectedResponse =
        new JsonRpcErrorResponse(null, JsonRpcError.NO_ACCOUNT_FOUND);

    when(blockchainQueries.headBlockNumber()).thenReturn(14L);

    final JsonRpcRequestContext request =
        requestWithParams(
            Address.fromHexString("0x0000000000000000000000000000000000000000"),
            new String[] {storageKey.toString()},
            String.valueOf(blockNumber));

    final JsonRpcErrorResponse response = (JsonRpcErrorResponse) method.response(request);

    assertThat(response).usingRecursiveComparison().isEqualTo(expectedResponse);
  }

  @Test
  public void errorWhenWorldStateUnavailable() {

    when(blockchainQueries.headBlockNumber()).thenReturn(14L);
    when(blockchainQueries.getWorldState(any())).thenReturn(Optional.empty());

    final JsonRpcErrorResponse expectedResponse =
        new JsonRpcErrorResponse(null, JsonRpcError.WORLD_STATE_UNAVAILABLE);

    final JsonRpcRequestContext request =
        requestWithParams(
            Address.fromHexString("0x0000000000000000000000000000000000000000"),
            new String[] {storageKey.toString()},
            String.valueOf(blockNumber));

    final JsonRpcErrorResponse response = (JsonRpcErrorResponse) method.response(request);

    assertThat(response).usingRecursiveComparison().isEqualTo(expectedResponse);
  }

  @Test
  public void getProof() {

    final GetProofResult expectedResponse = generateWorldState();

    when(blockchainQueries.headBlockNumber()).thenReturn(14L);

    final JsonRpcRequestContext request =
        requestWithParams(
            address.toString(), new String[] {storageKey.toString()}, String.valueOf(blockNumber));

    final JsonRpcSuccessResponse response = (JsonRpcSuccessResponse) method.response(request);

    assertThat(response.getResult()).usingRecursiveComparison().isEqualTo(expectedResponse);
  }

  private JsonRpcRequestContext requestWithParams(final Object... params) {
    return new JsonRpcRequestContext(new JsonRpcRequest(JSON_RPC_VERSION, ETH_METHOD, params));
  }

  private GetProofResult generateWorldState() {

    final Wei balance = Wei.of(1);
    final Hash codeHash =
        Hash.fromHexString("0xc5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470");
    final long nonce = 1;
    final Hash rootHash =
        Hash.fromHexString("0x56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b431");
    final Hash storageRoot =
        Hash.fromHexString("0x56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421");

    final WorldStateArchive worldStateArchive = mock(WorldStateArchive.class);

    when(blockchainQueries.getWorldStateArchive()).thenReturn(worldStateArchive);

    final StateTrieAccountValue stateTrieAccountValue = mock(StateTrieAccountValue.class);
    when(stateTrieAccountValue.getBalance()).thenReturn(balance);
    when(stateTrieAccountValue.getCodeHash()).thenReturn(codeHash);
    when(stateTrieAccountValue.getNonce()).thenReturn(nonce);
    when(stateTrieAccountValue.getStorageRoot()).thenReturn(storageRoot);

    final WorldStateProof worldStateProof = mock(WorldStateProof.class);
    when(worldStateProof.getAccountProof())
        .thenReturn(
            Collections.singletonList(
                Bytes.fromHexString(
                    "0x1111111111111111111111111111111111111111111111111111111111111111")));
    when(worldStateProof.getStateTrieAccountValue()).thenReturn(stateTrieAccountValue);
    when(worldStateProof.getStorageKeys()).thenReturn(Collections.singletonList(storageKey));
    when(worldStateProof.getStorageProof(storageKey))
        .thenReturn(
            Collections.singletonList(
                Bytes.fromHexString(
                    "0x2222222222222222222222222222222222222222222222222222222222222222")));
    when(worldStateProof.getStorageValue(storageKey)).thenReturn(UInt256.ZERO);

    when(worldStateArchive.getAccountProof(eq(rootHash), eq(address), anyList()))
        .thenReturn(Optional.of(worldStateProof));

    final MutableWorldState mutableWorldState = mock(MutableWorldState.class);
    when(mutableWorldState.rootHash()).thenReturn(rootHash);
    when(blockchainQueries.getWorldState(any())).thenReturn(Optional.of(mutableWorldState));

    return GetProofResult.buildGetProofResult(address, worldStateProof);
  }
}
