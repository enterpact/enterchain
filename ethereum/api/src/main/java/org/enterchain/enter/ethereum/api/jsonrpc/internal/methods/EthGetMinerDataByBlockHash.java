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

import org.enterchain.enter.ethereum.api.jsonrpc.RpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.results.ImmutableMinerDataResult;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.results.ImmutableUncleRewardResult;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.results.MinerDataResult;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.results.MinerDataResult.UncleRewardResult;
import org.enterchain.enter.ethereum.api.query.BlockWithMetadata;
import org.enterchain.enter.ethereum.api.query.BlockchainQueries;
import org.enterchain.enter.ethereum.api.query.TransactionReceiptWithMetadata;
import org.enterchain.enter.ethereum.api.query.TransactionWithMetadata;
import org.enterchain.enter.ethereum.core.BlockHeader;
import org.enterchain.enter.ethereum.core.Hash;
import org.enterchain.enter.ethereum.core.Wei;
import org.enterchain.enter.ethereum.mainnet.ProtocolSchedule;
import org.enterchain.enter.ethereum.mainnet.ProtocolSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import org.apache.tuweni.units.bigints.BaseUInt256Value;

public class EthGetMinerDataByBlockHash implements JsonRpcMethod {
  private final Supplier<BlockchainQueries> blockchain;
  private final ProtocolSchedule protocolSchedule;

  public EthGetMinerDataByBlockHash(
      final BlockchainQueries blockchain, final ProtocolSchedule protocolSchedule) {
    this(Suppliers.ofInstance(blockchain), protocolSchedule);
  }

  public EthGetMinerDataByBlockHash(
      final Supplier<BlockchainQueries> blockchain, final ProtocolSchedule protocolSchedule) {
    this.blockchain = blockchain;
    this.protocolSchedule = protocolSchedule;
  }

  @Override
  public String getName() {
    return RpcMethod.ETH_GET_MINER_DATA_BY_BLOCK_HASH.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    final Hash hash = requestContext.getRequest().getRequiredParameter(0, Hash.class);

    BlockWithMetadata<TransactionWithMetadata, Hash> block =
        blockchain.get().blockByHash(hash).orElse(null);

    MinerDataResult minerDataResult = null;
    if (block != null) {
      minerDataResult = createMinerDataResult(block, protocolSchedule, blockchain.get());
    }

    return new JsonRpcSuccessResponse(requestContext.getRequest().getId(), minerDataResult);
  }

  public static MinerDataResult createMinerDataResult(
      final BlockWithMetadata<TransactionWithMetadata, Hash> block,
      final ProtocolSchedule protocolSchedule,
      final BlockchainQueries blockchainQueries) {
    final BlockHeader blockHeader = block.getHeader();
    final ProtocolSpec protocolSpec = protocolSchedule.getByBlockNumber(blockHeader.getNumber());
    final Wei staticBlockReward = protocolSpec.getBlockReward();
    final Wei transactionFee =
        block.getTransactions().stream()
            .map(
                t -> {
                  Optional<TransactionReceiptWithMetadata> transactionReceiptWithMetadata =
                      blockchainQueries.transactionReceiptByTransactionHash(
                          t.getTransaction().getHash());
                  return t.getTransaction()
                      .getGasPrice()
                      .multiply(
                          transactionReceiptWithMetadata
                              .map(TransactionReceiptWithMetadata::getGasUsed)
                              .orElse(0L));
                })
            .reduce(Wei.ZERO, BaseUInt256Value::add);
    final Wei uncleInclusionReward =
        staticBlockReward.multiply(block.getOmmers().size()).divide(32);
    final Wei netBlockReward = staticBlockReward.add(transactionFee).add(uncleInclusionReward);
    final List<UncleRewardResult> uncleRewards = new ArrayList<>();
    blockchainQueries
        .getBlockchain()
        .getBlockByNumber(block.getHeader().getNumber())
        .ifPresent(
            blockBody ->
                blockBody
                    .getBody()
                    .getOmmers()
                    .forEach(
                        header ->
                            uncleRewards.add(
                                ImmutableUncleRewardResult.builder()
                                    .hash(header.getHash().toHexString())
                                    .coinbase(header.getCoinbase().toHexString())
                                    .build())));

    return ImmutableMinerDataResult.builder()
        .netBlockReward(netBlockReward.toHexString())
        .staticBlockReward(staticBlockReward.toHexString())
        .transactionFee(transactionFee.toHexString())
        .uncleInclusionReward(uncleInclusionReward.toHexString())
        .uncleRewards(uncleRewards)
        .coinbase(blockHeader.getCoinbase().toHexString())
        .extraData(blockHeader.getExtraData().toHexString())
        .difficulty(blockHeader.getDifficulty().toHexString())
        .totalDifficulty(block.getTotalDifficulty().toHexString())
        .build();
  }
}
