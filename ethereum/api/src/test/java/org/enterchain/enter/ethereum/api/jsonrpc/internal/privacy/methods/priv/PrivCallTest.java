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
package org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.priv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.enterchain.enter.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.parameters.JsonCallParameter;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.privacy.methods.EnclavePublicKeyProvider;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.results.Quantity;
import org.enterchain.enter.ethereum.api.query.BlockchainQueries;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.Gas;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.mainnet.ValidationResult;
import org.enterchain.enter.ethereum.privacy.DefaultPrivacyController;
import org.enterchain.enter.ethereum.privacy.MultiTenancyValidationException;
import org.enterchain.enter.ethereum.privacy.PrivacyController;
import org.enterchain.enter.ethereum.processing.TransactionProcessingResult;
import org.enterchain.enter.ethereum.transaction.CallParameter;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PrivCallTest {

  private PrivCall method;

  private static final String ENCLAVE_PUBLIC_KEY = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";

  @Mock private BlockchainQueries blockchainQueries;
  String privacyGroupId = "privacyGroupId";
  private final EnclavePublicKeyProvider enclavePublicKeyProvider = (user) -> ENCLAVE_PUBLIC_KEY;
  private final PrivacyController privacyController = mock(DefaultPrivacyController.class);

  @Before
  public void setUp() {
    method = new PrivCall(blockchainQueries, privacyController, enclavePublicKeyProvider);
  }

  @Test
  public void shouldReturnCorrectMethodName() {
    assertThat(method.getName()).isEqualTo("priv_call");
  }

  @Test
  public void shouldThrowInvalidJsonRpcParametersExceptionWhenMissingToField() {
    final JsonCallParameter callParameter =
        new JsonCallParameter(
            Address.fromHexString("0x0"),
            null,
            Gas.ZERO,
            Wei.ZERO,
            null,
            null,
            Wei.ZERO,
            Bytes.EMPTY,
            null);
    final JsonRpcRequestContext request = ethCallRequest(privacyGroupId, callParameter, "latest");

    final Throwable thrown = catchThrowable(() -> method.response(request));

    assertThat(thrown)
        .isInstanceOf(InvalidJsonRpcParameters.class)
        .hasNoCause()
        .hasMessage("Missing \"to\" field in call arguments");
  }

  @Test
  public void shouldReturnNullWhenProcessorReturnsEmpty() {
    final JsonRpcRequestContext request = ethCallRequest(privacyGroupId, callParameter(), "latest");
    final JsonRpcResponse expectedResponse = new JsonRpcSuccessResponse(null, null);

    final JsonRpcResponse response = method.response(request);

    assertThat(response).isEqualToComparingFieldByField(expectedResponse);
    verify(privacyController).simulatePrivateTransaction(any(), any(), any(), anyLong());
  }

  @Test
  public void shouldAcceptRequestWhenMissingOptionalFields() {
    final JsonCallParameter callParameter =
        new JsonCallParameter(
            null, Address.fromHexString("0x0"), null, null, null, null, null, null, null);
    final JsonRpcRequestContext request = ethCallRequest(privacyGroupId, callParameter, "latest");
    final JsonRpcResponse expectedResponse =
        new JsonRpcSuccessResponse(null, Bytes.of().toString());

    mockTransactionProcessorSuccessResult(Bytes.of());

    final JsonRpcResponse response = method.response(request);

    assertThat(response).isEqualToComparingFieldByFieldRecursively(expectedResponse);
    verify(privacyController)
        .simulatePrivateTransaction(any(), any(), eq(callParameter), anyLong());
  }

  @Test
  public void shouldReturnExecutionResultWhenExecutionIsSuccessful() {
    final JsonRpcRequestContext request = ethCallRequest(privacyGroupId, callParameter(), "latest");
    final JsonRpcResponse expectedResponse =
        new JsonRpcSuccessResponse(null, Bytes.of(1).toString());
    mockTransactionProcessorSuccessResult(Bytes.of(1));

    final JsonRpcResponse response = method.response(request);

    assertThat(response).isEqualToComparingFieldByFieldRecursively(expectedResponse);
    verify(privacyController)
        .simulatePrivateTransaction(any(), any(), eq(callParameter()), anyLong());
  }

  @Test
  public void shouldUseCorrectBlockNumberWhenLatest() {
    final JsonRpcRequestContext request = ethCallRequest(privacyGroupId, callParameter(), "latest");
    when(blockchainQueries.headBlockNumber()).thenReturn(11L);

    method.response(request);

    verify(privacyController).simulatePrivateTransaction(any(), any(), any(), eq(11L));
  }

  @Test
  public void shouldUseCorrectBlockNumberWhenEarliest() {
    final JsonRpcRequestContext request =
        ethCallRequest(privacyGroupId, callParameter(), "earliest");
    method.response(request);

    verify(privacyController).simulatePrivateTransaction(any(), any(), any(), eq(0L));
  }

  @Test
  public void shouldUseCorrectBlockNumberWhenSpecified() {
    final JsonRpcRequestContext request =
        ethCallRequest(privacyGroupId, callParameter(), Quantity.create(13L));

    method.response(request);

    verify(privacyController).simulatePrivateTransaction(any(), any(), any(), eq(13L));
  }

  @Test
  public void shouldThrowCorrectExceptionWhenNoPrivacyGroupSpecified() {
    final JsonRpcRequestContext request =
        ethCallRequest(null, callParameter(), Quantity.create(13L));
    final Throwable thrown = catchThrowable(() -> method.response(request));

    assertThat(thrown)
        .isInstanceOf(InvalidJsonRpcParameters.class)
        .hasNoCause()
        .hasMessage("Missing required json rpc parameter at index 0");
  }

  @Test
  public void multiTenancyCheckFailure() {
    doThrow(new MultiTenancyValidationException("msg"))
        .when(privacyController)
        .verifyPrivacyGroupContainsEnclavePublicKey(
            eq(privacyGroupId), eq(ENCLAVE_PUBLIC_KEY), eq(Optional.of(1L)));

    final JsonRpcRequestContext request = ethCallRequest(privacyGroupId, callParameter(), "0x02");

    assertThatThrownBy(() -> method.response(request))
        .isInstanceOf(MultiTenancyValidationException.class);
  }

  private JsonCallParameter callParameter() {
    return new JsonCallParameter(
        Address.fromHexString("0x0"),
        Address.fromHexString("0x0"),
        Gas.ZERO,
        Wei.ZERO,
        null,
        null,
        Wei.ZERO,
        Bytes.EMPTY,
        null);
  }

  private JsonRpcRequestContext ethCallRequest(
      final String privacyGroupId,
      final CallParameter callParameter,
      final String blockNumberInHex) {
    return new JsonRpcRequestContext(
        new JsonRpcRequest(
            "2.0", "priv_call", new Object[] {privacyGroupId, callParameter, blockNumberInHex}));
  }

  private void mockTransactionProcessorSuccessResult(final Bytes output) {
    final TransactionProcessingResult result = mock(TransactionProcessingResult.class);

    when(result.getValidationResult()).thenReturn(ValidationResult.valid());
    when(result.getOutput()).thenReturn(output);
    when(privacyController.simulatePrivateTransaction(any(), any(), any(), anyLong()))
        .thenReturn(Optional.of(result));
  }
}
