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
 *
 */
package org.enterchain.enter.ethereum.referencetests;

import static org.enterchain.enter.ethereum.core.InMemoryKeyValueStorageProvider.createInMemoryWorldStateArchive;

import org.enterchain.enter.ethereum.ProtocolContext;
import org.enterchain.enter.ethereum.chain.MutableBlockchain;
import org.enterchain.enter.ethereum.core.Address;
import org.enterchain.enter.ethereum.core.Block;
import org.enterchain.enter.ethereum.core.BlockBody;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.BlockHeaderFunctions;
import org.enterchain.enter.ethereum.core.Difficulty;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.core.InMemoryKeyValueStorageProvider;
import org.enterchain.enter.ethereum.core.LogsBloomFilter;
import org.enterchain.enter.ethereum.core.MutableWorldState;
import org.enterchain.enter.ethereum.core.ParsedExtraData;
import org.enterchain.enter.ethereum.core.Transaction;
import org.enterchain.enter.ethereum.core.WorldUpdater;
import org.enterchain.enter.ethereum.mainnet.MainnetBlockHeaderFunctions;
import org.enterchain.enter.ethereum.rlp.BytesValueRLPInput;
import org.enterchain.enter.ethereum.rlp.RLPInput;
import org.enterchain.enter.ethereum.worldstate.WorldStateArchive;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.tuweni.bytes.Bytes;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockchainReferenceTestCaseSpec {

  private final String network;

  private final CandidateBlock[] candidateBlocks;

  private final ReferenceTestBlockHeader genesisBlockHeader;

  private final Hash lastBlockHash;

  private final WorldStateArchive worldStateArchive;

  private final MutableBlockchain blockchain;
  private final String sealEngine;

  private final ProtocolContext protocolContext;

  private static WorldStateArchive buildWorldStateArchive(
      final Map<String, ReferenceTestWorldState.AccountMock> accounts) {
    final WorldStateArchive worldStateArchive = createInMemoryWorldStateArchive();

    final MutableWorldState worldState = worldStateArchive.getMutable();
    final WorldUpdater updater = worldState.updater();

    for (final Map.Entry<String, ReferenceTestWorldState.AccountMock> entry : accounts.entrySet()) {
      ReferenceTestWorldState.insertAccount(
          updater, Address.fromHexString(entry.getKey()), entry.getValue());
    }

    updater.commit();
    worldState.persist(null);

    return worldStateArchive;
  }

  private static MutableBlockchain buildBlockchain(final BlockHeader genesisBlockHeader) {
    final Block genesisBlock = new Block(genesisBlockHeader, BlockBody.empty());
    return InMemoryKeyValueStorageProvider.createInMemoryBlockchain(genesisBlock);
  }

  @JsonCreator
  public BlockchainReferenceTestCaseSpec(
      @JsonProperty("network") final String network,
      @JsonProperty("blocks") final CandidateBlock[] candidateBlocks,
      @JsonProperty("genesisBlockHeader") final ReferenceTestBlockHeader genesisBlockHeader,
      @SuppressWarnings("unused") @JsonProperty("genesisRLP") final String genesisRLP,
      @JsonProperty("pre") final Map<String, ReferenceTestWorldState.AccountMock> accounts,
      @JsonProperty("lastblockhash") final String lastBlockHash,
      @JsonProperty("sealEngine") final String sealEngine) {
    this.network = network;
    this.candidateBlocks = candidateBlocks;
    this.genesisBlockHeader = genesisBlockHeader;
    this.lastBlockHash = Hash.fromHexString(lastBlockHash);
    this.worldStateArchive = buildWorldStateArchive(accounts);
    this.blockchain = buildBlockchain(genesisBlockHeader);
    this.sealEngine = sealEngine;
    this.protocolContext = new ProtocolContext(this.blockchain, this.worldStateArchive, null);
  }

  public String getNetwork() {
    return network;
  }

  public CandidateBlock[] getCandidateBlocks() {
    return candidateBlocks;
  }

  public WorldStateArchive getWorldStateArchive() {
    return worldStateArchive;
  }

  public BlockHeader getGenesisBlockHeader() {
    return genesisBlockHeader;
  }

  public MutableBlockchain getBlockchain() {
    return blockchain;
  }

  public ProtocolContext getProtocolContext() {
    return protocolContext;
  }

  public Hash getLastBlockHash() {
    return lastBlockHash;
  }

  public String getSealEngine() {
    return sealEngine;
  }

  public static class ReferenceTestBlockHeader extends BlockHeader {

    @JsonCreator
    public ReferenceTestBlockHeader(
        @JsonProperty("parentHash") final String parentHash,
        @JsonProperty("uncleHash") final String uncleHash,
        @JsonProperty("coinbase") final String coinbase,
        @JsonProperty("stateRoot") final String stateRoot,
        @JsonProperty("transactionsTrie") final String transactionsTrie,
        @JsonProperty("receiptTrie") final String receiptTrie,
        @JsonProperty("bloom") final String bloom,
        @JsonProperty("difficulty") final String difficulty,
        @JsonProperty("number") final String number,
        @JsonProperty("gasLimit") final String gasLimit,
        @JsonProperty("gasUsed") final String gasUsed,
        @JsonProperty("timestamp") final String timestamp,
        @JsonProperty("extraData") final String extraData,
        @JsonProperty("baseFee") final String baseFee,
        @JsonProperty("mixHash") final String mixHash,
        @JsonProperty("nonce") final String nonce,
        @JsonProperty("hash") final String hash) {
      super(
          Hash.fromHexString(parentHash), // parentHash
          Hash.fromHexString(uncleHash), // ommersHash
          Address.fromHexString(coinbase), // coinbase
          Hash.fromHexString(stateRoot), // stateRoot
          Hash.fromHexString(transactionsTrie), // transactionsRoot
          Hash.fromHexString(receiptTrie), // receiptTrie
          LogsBloomFilter.fromHexString(bloom), // bloom
          Difficulty.fromHexString(difficulty), // difficulty
          Long.decode(number), // number
          Long.decode(gasLimit), // gasLimit
          Long.decode(gasUsed), // gasUsed
          Long.decode(timestamp), // timestamp
          Bytes.fromHexString(extraData), // extraData
          baseFee != null ? Long.decode(baseFee) : null, // baseFee
          Hash.fromHexString(mixHash), // mixHash
          Bytes.fromHexString(nonce).getLong(0),
          new BlockHeaderFunctions() {
            @Override
            public Hash hash(final BlockHeader header) {
              return Hash.fromHexString(hash);
            }

            @Override
            public ParsedExtraData parseExtraData(final BlockHeader header) {
              return null;
            }
          });
    }
  }

  @JsonIgnoreProperties({
    "expectExceptionByzantium",
    "expectExceptionConstantinople",
    "expectExceptionConstantinopleFix",
    "expectExceptionIstanbul",
    "expectExceptionEIP150",
    "expectExceptionEIP158",
    "expectExceptionFrontier",
    "expectExceptionHomestead",
    "expectException",
    "blocknumber",
    "chainname",
    "expectExceptionALL",
    "chainnetwork"
  })
  public static class CandidateBlock {

    private final Bytes rlp;

    private final Boolean valid;

    @JsonCreator
    public CandidateBlock(
        @JsonProperty("rlp") final String rlp,
        @JsonProperty("blockHeader") final Object blockHeader,
        @JsonProperty("transactions") final Object transactions,
        @JsonProperty("uncleHeaders") final Object uncleHeaders) {
      boolean valid = true;
      // The BLOCK__WrongCharAtRLP_0 test has an invalid character in its rlp string.
      Bytes rlpAttempt = null;
      try {
        rlpAttempt = Bytes.fromHexString(rlp);
      } catch (final IllegalArgumentException e) {
        valid = false;
      }
      this.rlp = rlpAttempt;

      if (blockHeader == null && transactions == null && uncleHeaders == null) {
        valid = false;
      }

      this.valid = valid;
    }

    public boolean isValid() {
      return valid;
    }

    public boolean isExecutable() {
      return rlp != null;
    }

    public Block getBlock() {
      final RLPInput input = new BytesValueRLPInput(rlp, false);
      input.enterList();
      final MainnetBlockHeaderFunctions blockHeaderFunctions = new MainnetBlockHeaderFunctions();
      final BlockHeader header = BlockHeader.readFrom(input, blockHeaderFunctions);
      final BlockBody body =
          new BlockBody(
              input.readList(Transaction::readFrom),
              input.readList(rlp -> BlockHeader.readFrom(rlp, blockHeaderFunctions)));
      return new Block(header, body);
    }
  }
}
