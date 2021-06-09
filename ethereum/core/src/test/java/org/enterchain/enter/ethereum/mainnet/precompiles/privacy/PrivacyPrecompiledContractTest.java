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
package org.enterchain.enter.ethereum.mainnet.precompiles.privacy;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.enterchain.enter.ethereum.core.PrivateTransactionDataFixture.VALID_BASE64_ENCLAVE_KEY;
import static org.enterchain.enter.ethereum.core.PrivateTransactionDataFixture.privateTransactionBesu;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.enterchain.enter.enclave.Enclave;
import org.enterchain.enter.enclave.EnclaveClientException;
import org.enterchain.enter.enclave.EnclaveConfigurationException;
import org.enterchain.enter.enclave.types.PrivacyGroup;
import org.enterchain.enter.enclave.types.ReceiveResponse;
import org.enterchain.enter.ethereum.chain.Blockchain;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.BlockDataGenerator;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.core.Log;
import org.enterchain.enter.ethereum.core.MutableWorldState;
import org.enterchain.enter.ethereum.core.ProcessableBlockHeader;
import org.enterchain.enter.ethereum.core.WorldUpdater;
import org.enterchain.enter.ethereum.mainnet.SpuriousDragonGasCalculator;
import org.enterchain.enter.ethereum.mainnet.ValidationResult;
import org.enterchain.enter.ethereum.privacy.PrivateStateRootResolver;
import org.enterchain.enter.ethereum.privacy.PrivateTransaction;
import org.enterchain.enter.ethereum.privacy.PrivateTransactionProcessor;
import org.enterchain.enter.ethereum.privacy.storage.PrivacyGroupHeadBlockMap;
import org.enterchain.enter.ethereum.privacy.storage.PrivateBlockMetadata;
import org.enterchain.enter.ethereum.privacy.storage.PrivateMetadataUpdater;
import org.enterchain.enter.ethereum.privacy.storage.PrivateStateStorage;
import org.enterchain.enter.ethereum.processing.TransactionProcessingResult;
import org.enterchain.enter.ethereum.rlp.BytesValueRLPOutput;
import org.enterchain.enter.ethereum.transaction.TransactionInvalidReason;
import org.enterchain.enter.ethereum.vm.BlockHashLookup;
import org.enterchain.enter.ethereum.vm.MessageFrame;
import org.enterchain.enter.ethereum.vm.OperationTracer;
import org.enterchain.enter.ethereum.worldstate.WorldStateArchive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PrivacyPrecompiledContractTest {

  @Rule public final TemporaryFolder temp = new TemporaryFolder();

  private final String actual = "Test String";
  private final Bytes privateTransactionLookupId = Bytes.wrap(actual.getBytes(UTF_8));
  private MessageFrame messageFrame;
  private Blockchain blockchain;
  private final String DEFAULT_OUTPUT = "0x01";
  final String PAYLOAD_TEST_PRIVACY_GROUP_ID = "8lDVI66RZHIrBsolz6Kn88Rd+WsJ4hUjb4hsh29xW/o=";
  private final WorldStateArchive worldStateArchive = mock(WorldStateArchive.class);
  final PrivateStateStorage privateStateStorage = mock(PrivateStateStorage.class);
  final PrivateMetadataUpdater privateMetadataUpdater = mock(PrivateMetadataUpdater.class);
  final PrivateStateRootResolver privateStateRootResolver =
      new PrivateStateRootResolver(privateStateStorage);

  private PrivateTransactionProcessor mockPrivateTxProcessor(
      final TransactionProcessingResult result) {
    final PrivateTransactionProcessor mockPrivateTransactionProcessor =
        mock(PrivateTransactionProcessor.class);
    when(mockPrivateTransactionProcessor.processTransaction(
            nullable(Blockchain.class),
            nullable(WorldUpdater.class),
            nullable(WorldUpdater.class),
            nullable(ProcessableBlockHeader.class),
            nullable((Hash.class)),
            nullable(PrivateTransaction.class),
            nullable(Address.class),
            nullable(OperationTracer.class),
            nullable(BlockHashLookup.class),
            nullable(Bytes.class)))
        .thenReturn(result);

    return mockPrivateTransactionProcessor;
  }

  @Before
  public void setUp() {
    final MutableWorldState mutableWorldState = mock(MutableWorldState.class);
    when(mutableWorldState.updater()).thenReturn(mock(WorldUpdater.class));
    when(worldStateArchive.getMutable()).thenReturn(mutableWorldState);
    when(worldStateArchive.getMutable(any(), any())).thenReturn(Optional.of(mutableWorldState));

    when(privateMetadataUpdater.getPrivacyGroupHeadBlockMap())
        .thenReturn(PrivacyGroupHeadBlockMap.empty());
    when(privateMetadataUpdater.getPrivateBlockMetadata(any()))
        .thenReturn(new PrivateBlockMetadata(Collections.emptyList()));

    messageFrame = mock(MessageFrame.class);
    blockchain = mock(Blockchain.class);
    final BlockDataGenerator blockGenerator = new BlockDataGenerator();
    final Block genesis = blockGenerator.genesisBlock();
    final Block block =
        blockGenerator.block(
            new BlockDataGenerator.BlockOptions().setParentHash(genesis.getHeader().getHash()));
    when(blockchain.getGenesisBlock()).thenReturn(genesis);
    when(blockchain.getBlockByHash(block.getHash())).thenReturn(Optional.of(block));
    when(blockchain.getBlockByHash(genesis.getHash())).thenReturn(Optional.of(genesis));
    when(messageFrame.getBlockchain()).thenReturn(blockchain);
    when(messageFrame.getBlockHeader()).thenReturn(block.getHeader());
    final PrivateMetadataUpdater privateMetadataUpdater = mock(PrivateMetadataUpdater.class);
    when(messageFrame.getPrivateMetadataUpdater()).thenReturn(privateMetadataUpdater);
    when(privateMetadataUpdater.getPrivacyGroupHeadBlockMap())
        .thenReturn(PrivacyGroupHeadBlockMap.empty());
  }

  @Test
  public void testPayloadFoundInEnclave() {
    final Enclave enclave = mock(Enclave.class);
    when(enclave.retrievePrivacyGroup(PAYLOAD_TEST_PRIVACY_GROUP_ID))
        .thenReturn(
            new PrivacyGroup(
                PAYLOAD_TEST_PRIVACY_GROUP_ID,
                PrivacyGroup.Type.PANTHEON,
                "",
                "",
                Arrays.asList(VALID_BASE64_ENCLAVE_KEY.toBase64String())));
    final PrivacyPrecompiledContract contract = buildPrivacyPrecompiledContract(enclave);
    final List<Log> logs = new ArrayList<>();
    contract.setPrivateTransactionProcessor(
        mockPrivateTxProcessor(
            TransactionProcessingResult.successful(
                logs, 0, 0, Bytes.fromHexString(DEFAULT_OUTPUT), null)));

    final PrivateTransaction privateTransaction = privateTransactionBesu();
    final byte[] payload = convertPrivateTransactionToBytes(privateTransaction);
    final String privateFrom = privateTransaction.getPrivateFrom().toBase64String();

    final ReceiveResponse response =
        new ReceiveResponse(payload, PAYLOAD_TEST_PRIVACY_GROUP_ID, privateFrom);
    when(enclave.receive(any(String.class))).thenReturn(response);

    final Bytes actual = contract.compute(privateTransactionLookupId, messageFrame);

    assertThat(actual).isEqualTo(Bytes.fromHexString(DEFAULT_OUTPUT));
  }

  @Test
  public void testPayloadNotFoundInEnclave() {
    final Enclave enclave = mock(Enclave.class);
    final PrivacyPrecompiledContract contract = buildPrivacyPrecompiledContract(enclave);

    when(enclave.receive(any(String.class))).thenThrow(EnclaveClientException.class);

    final Bytes expected = contract.compute(privateTransactionLookupId, messageFrame);
    assertThat(expected).isEqualTo(Bytes.EMPTY);
  }

  @Test(expected = RuntimeException.class)
  public void testEnclaveDown() {
    final Enclave enclave = mock(Enclave.class);
    final PrivacyPrecompiledContract contract = buildPrivacyPrecompiledContract(enclave);

    when(enclave.receive(any(String.class))).thenThrow(new RuntimeException());

    contract.compute(privateTransactionLookupId, messageFrame);
  }

  @Test
  public void testEnclaveBelowRequiredVersion() {
    final Enclave enclave = mock(Enclave.class);
    final PrivacyPrecompiledContract contract = buildPrivacyPrecompiledContract(enclave);
    final PrivateTransaction privateTransaction = privateTransactionBesu();
    final byte[] payload = convertPrivateTransactionToBytes(privateTransaction);

    final ReceiveResponse responseWithoutSenderKey =
        new ReceiveResponse(payload, PAYLOAD_TEST_PRIVACY_GROUP_ID, null);
    when(enclave.receive(eq(privateTransactionLookupId.toBase64String())))
        .thenReturn(responseWithoutSenderKey);

    assertThatThrownBy(() -> contract.compute(privateTransactionLookupId, messageFrame))
        .isInstanceOf(EnclaveConfigurationException.class)
        .hasMessage("Incompatible Orion version. Orion version must be 1.6.0 or greater.");
  }

  @Test
  public void testPrivateTransactionWithoutPrivateFrom() {
    final Enclave enclave = mock(Enclave.class);
    final PrivacyPrecompiledContract contract = buildPrivacyPrecompiledContract(enclave);
    final PrivateTransaction privateTransaction = spy(privateTransactionBesu());
    when(privateTransaction.getPrivateFrom()).thenReturn(Bytes.EMPTY);
    final byte[] payload = convertPrivateTransactionToBytes(privateTransaction);

    final String senderKey = privateTransaction.getPrivateFrom().toBase64String();
    final ReceiveResponse response =
        new ReceiveResponse(payload, PAYLOAD_TEST_PRIVACY_GROUP_ID, senderKey);
    when(enclave.receive(eq(privateTransactionLookupId.toBase64String()))).thenReturn(response);

    final Bytes expected = contract.compute(privateTransactionLookupId, messageFrame);
    assertThat(expected).isEqualTo(Bytes.EMPTY);
  }

  @Test
  public void testPayloadNotMatchingPrivateFrom() {
    final Enclave enclave = mock(Enclave.class);
    final PrivacyPrecompiledContract contract = buildPrivacyPrecompiledContract(enclave);
    final PrivateTransaction privateTransaction = privateTransactionBesu();
    final byte[] payload = convertPrivateTransactionToBytes(privateTransaction);

    final String wrongSenderKey = Bytes.random(32).toBase64String();
    final ReceiveResponse responseWithWrongSenderKey =
        new ReceiveResponse(payload, PAYLOAD_TEST_PRIVACY_GROUP_ID, wrongSenderKey);
    when(enclave.receive(eq(privateTransactionLookupId.toBase64String())))
        .thenReturn(responseWithWrongSenderKey);

    final Bytes expected = contract.compute(privateTransactionLookupId, messageFrame);
    assertThat(expected).isEqualTo(Bytes.EMPTY);
  }

  @Test
  public void testPrivateFromNotMemberOfGroup() {
    final Enclave enclave = mock(Enclave.class);
    when(enclave.retrievePrivacyGroup(PAYLOAD_TEST_PRIVACY_GROUP_ID))
        .thenReturn(
            new PrivacyGroup(
                PAYLOAD_TEST_PRIVACY_GROUP_ID,
                PrivacyGroup.Type.PANTHEON,
                "",
                "",
                Arrays.asList(VALID_BASE64_ENCLAVE_KEY.toBase64String())));
    final PrivacyPrecompiledContract contract = buildPrivacyPrecompiledContract(enclave);
    contract.setPrivateTransactionProcessor(
        mockPrivateTxProcessor(
            TransactionProcessingResult.successful(
                new ArrayList<>(), 0, 0, Bytes.fromHexString(DEFAULT_OUTPUT), null)));

    final PrivateTransaction privateTransaction = privateTransactionBesu();
    final byte[] payload = convertPrivateTransactionToBytes(privateTransaction);
    final String privateFrom = privateTransaction.getPrivateFrom().toBase64String();

    final ReceiveResponse response =
        new ReceiveResponse(payload, PAYLOAD_TEST_PRIVACY_GROUP_ID, privateFrom);
    when(enclave.receive(any(String.class))).thenReturn(response);

    final Bytes actual = contract.compute(privateTransactionLookupId, messageFrame);

    assertThat(actual).isEqualTo(Bytes.fromHexString(DEFAULT_OUTPUT));
  }

  @Test
  public void testInvalidPrivateTransaction() {
    final Enclave enclave = mock(Enclave.class);
    when(enclave.retrievePrivacyGroup(PAYLOAD_TEST_PRIVACY_GROUP_ID))
        .thenReturn(
            new PrivacyGroup(
                PAYLOAD_TEST_PRIVACY_GROUP_ID,
                PrivacyGroup.Type.PANTHEON,
                "",
                "",
                Arrays.asList(VALID_BASE64_ENCLAVE_KEY.toBase64String())));
    final PrivacyPrecompiledContract contract =
        new PrivacyPrecompiledContract(
            new SpuriousDragonGasCalculator(),
            enclave,
            worldStateArchive,
            privateStateRootResolver);

    contract.setPrivateTransactionProcessor(
        mockPrivateTxProcessor(
            TransactionProcessingResult.invalid(
                ValidationResult.invalid(TransactionInvalidReason.INCORRECT_NONCE))));

    final PrivateTransaction privateTransaction = privateTransactionBesu();
    final byte[] payload = convertPrivateTransactionToBytes(privateTransaction);
    final String privateFrom = privateTransaction.getPrivateFrom().toBase64String();

    final ReceiveResponse response =
        new ReceiveResponse(payload, PAYLOAD_TEST_PRIVACY_GROUP_ID, privateFrom);

    when(enclave.receive(any(String.class))).thenReturn(response);

    final Bytes actual = contract.compute(privateTransactionLookupId, messageFrame);

    assertThat(actual).isEqualTo(Bytes.EMPTY);
  }

  @Test
  public void testSimulatedPublicTransactionIsSkipped() {
    final PrivacyPrecompiledContract emptyContract =
        new PrivacyPrecompiledContract(null, null, null, null);

    // A simulated public transaction doesn't contain a PrivateMetadataUpdater
    final MessageFrame frame = mock(MessageFrame.class);
    when(frame.getPrivateMetadataUpdater()).thenReturn(null);

    final Bytes result = emptyContract.compute(null, frame);
    assertThat(result).isEqualTo(Bytes.EMPTY);
  }

  private byte[] convertPrivateTransactionToBytes(final PrivateTransaction privateTransaction) {
    final BytesValueRLPOutput bytesValueRLPOutput = new BytesValueRLPOutput();
    privateTransaction.writeTo(bytesValueRLPOutput);

    return bytesValueRLPOutput.encoded().toBase64String().getBytes(UTF_8);
  }

  private PrivacyPrecompiledContract buildPrivacyPrecompiledContract(final Enclave enclave) {
    return new PrivacyPrecompiledContract(
        new SpuriousDragonGasCalculator(), enclave, worldStateArchive, privateStateRootResolver);
  }
}
