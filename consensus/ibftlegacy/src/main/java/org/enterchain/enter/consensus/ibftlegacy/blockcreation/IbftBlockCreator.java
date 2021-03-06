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
package org.enterchain.enter.consensus.ibftlegacy.blockcreation;

import org.enterchain.enter.consensus.ibftlegacy.IbftBlockHashing;
import org.enterchain.enter.consensus.ibftlegacy.IbftExtraData;
import org.enterchain.enter.consensus.ibftlegacy.IbftHelpers;
import org.enterchain.enter.crypto.KeyPair;
import org.enterchain.enter.crypto.SignatureAlgorithmFactory;
import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.blockcreation.AbstractBlockCreator;
import org.enterchain.enter.ethereum.blockcreation.GasLimitCalculator;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.BlockHeaderBuilder;
import org.enterchain.enter.ethereum.core.BlockHeaderFunctions;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.core.SealableBlockHeader;
import org.enterchain.enter.ethereum.core.Util;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.eth.transactions.PendingTransactions;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.mainnet.ScheduleBasedBlockHeaderFunctions;

/**
 * Responsible for producing a Block which conforms to IBFT validation rules (other than missing
 * commit seals). Transactions and associated Hashes (stateroot, receipts etc.) are loaded into the
 * Block in the base class as part of the transaction selection process.
 */
public class IbftBlockCreator extends AbstractBlockCreator {

  private final KeyPair nodeKeys;

  public IbftBlockCreator(
      final Address coinbase,
      final ExtraDataCalculator extraDataCalculator,
      final PendingTransactions pendingTransactions,
      final ProtocolContext protocolContext,
      final ProtocolSchedule protocolSchedule,
      final GasLimitCalculator gasLimitCalculator,
      final KeyPair nodeKeys,
      final Wei minTransactionGasPrice,
      final Double minBlockOccupancyRatio,
      final BlockHeader parentHeader) {
    super(
        coinbase,
        extraDataCalculator,
        pendingTransactions,
        protocolContext,
        protocolSchedule,
        gasLimitCalculator,
        minTransactionGasPrice,
        Util.publicKeyToAddress(nodeKeys.getPublicKey()),
        minBlockOccupancyRatio,
        parentHeader);
    this.nodeKeys = nodeKeys;
  }

  /**
   * Responsible for signing (hash of) the block (including MixHash and Nonce), and then injecting
   * the seal into the extraData. This is called after a suitable set of transactions have been
   * identified, and all resulting hashes have been inserted into the passed-in SealableBlockHeader.
   *
   * @param sealableBlockHeader A block header containing StateRoots, TransactionHashes etc.
   * @return The blockhead which is to be added to the block being proposed.
   */
  @Override
  protected BlockHeader createFinalBlockHeader(final SealableBlockHeader sealableBlockHeader) {

    final BlockHeaderFunctions blockHeaderFunctions =
        ScheduleBasedBlockHeaderFunctions.create(protocolSchedule);

    final BlockHeaderBuilder builder =
        BlockHeaderBuilder.create()
            .populateFrom(sealableBlockHeader)
            .mixHash(IbftHelpers.EXPECTED_MIX_HASH)
            .nonce(0)
            .blockHeaderFunctions(blockHeaderFunctions);

    final IbftExtraData sealedExtraData = constructSignedExtraData(builder.buildBlockHeader());

    // Replace the extraData in the BlockHeaderBuilder, and return header.
    return builder.extraData(sealedExtraData.encode()).buildBlockHeader();
  }

  /**
   * Produces an IbftExtraData object with a populated proposerSeal. The signature in the block is
   * generated from the Hash of the header (minus proposer and committer seals) and the nodeKeys.
   *
   * @param headerToSign An almost fully populated header (proposer and committer seals are empty)
   * @return Extra data containing the same vanity data and validators as extraData, however
   *     proposerSeal will also be populated.
   */
  private IbftExtraData constructSignedExtraData(final BlockHeader headerToSign) {
    final IbftExtraData extraData = IbftExtraData.decode(headerToSign);
    final Hash hashToSign =
        IbftBlockHashing.calculateDataHashForProposerSeal(headerToSign, extraData);
    return new IbftExtraData(
        extraData.getVanityData(),
        extraData.getSeals(),
        SignatureAlgorithmFactory.getInstance().sign(hashToSign, nodeKeys),
        extraData.getValidators());
  }
}
