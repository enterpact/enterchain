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
package org.enterchain.enter.ethereum.mainnet;

import org.enterchain.enter.ethereum.core.Account;
import org.enterchain.enter.ethereum.vm.EVM;
import org.enterchain.enter.ethereum.vm.GasCalculator;
import org.enterchain.enter.ethereum.vm.OperationRegistry;
import org.enterchain.enter.ethereum.vm.operations.AddModOperation;
import org.enterchain.enter.ethereum.vm.operations.AddOperation;
import org.enterchain.enter.ethereum.vm.operations.AddressOperation;
import org.enterchain.enter.ethereum.vm.operations.AndOperation;
import org.enterchain.enter.ethereum.vm.operations.BalanceOperation;
import org.enterchain.enter.ethereum.vm.operations.BaseFeeOperation;
import org.enterchain.enter.ethereum.vm.operations.BlockHashOperation;
import org.enterchain.enter.ethereum.vm.operations.ByteOperation;
import org.enterchain.enter.ethereum.vm.operations.CallCodeOperation;
import org.enterchain.enter.ethereum.vm.operations.CallDataCopyOperation;
import org.enterchain.enter.ethereum.vm.operations.CallDataLoadOperation;
import org.enterchain.enter.ethereum.vm.operations.CallDataSizeOperation;
import org.enterchain.enter.ethereum.vm.operations.CallOperation;
import org.enterchain.enter.ethereum.vm.operations.CallValueOperation;
import org.enterchain.enter.ethereum.vm.operations.CallerOperation;
import org.enterchain.enter.ethereum.vm.operations.ChainIdOperation;
import org.enterchain.enter.ethereum.vm.operations.CodeCopyOperation;
import org.enterchain.enter.ethereum.vm.operations.CodeSizeOperation;
import org.enterchain.enter.ethereum.vm.operations.CoinbaseOperation;
import org.enterchain.enter.ethereum.vm.operations.Create2Operation;
import org.enterchain.enter.ethereum.vm.operations.CreateOperation;
import org.enterchain.enter.ethereum.vm.operations.DelegateCallOperation;
import org.enterchain.enter.ethereum.vm.operations.DifficultyOperation;
import org.enterchain.enter.ethereum.vm.operations.DivOperation;
import org.enterchain.enter.ethereum.vm.operations.DupOperation;
import org.enterchain.enter.ethereum.vm.operations.EqOperation;
import org.enterchain.enter.ethereum.vm.operations.ExpOperation;
import org.enterchain.enter.ethereum.vm.operations.ExtCodeCopyOperation;
import org.enterchain.enter.ethereum.vm.operations.ExtCodeHashOperation;
import org.enterchain.enter.ethereum.vm.operations.ExtCodeSizeOperation;
import org.enterchain.enter.ethereum.vm.operations.GasLimitOperation;
import org.enterchain.enter.ethereum.vm.operations.GasOperation;
import org.enterchain.enter.ethereum.vm.operations.GasPriceOperation;
import org.enterchain.enter.ethereum.vm.operations.GtOperation;
import org.enterchain.enter.ethereum.vm.operations.InvalidOperation;
import org.enterchain.enter.ethereum.vm.operations.IsZeroOperation;
import org.enterchain.enter.ethereum.vm.operations.JumpDestOperation;
import org.enterchain.enter.ethereum.vm.operations.JumpOperation;
import org.enterchain.enter.ethereum.vm.operations.JumpiOperation;
import org.enterchain.enter.ethereum.vm.operations.LogOperation;
import org.enterchain.enter.ethereum.vm.operations.LtOperation;
import org.enterchain.enter.ethereum.vm.operations.MLoadOperation;
import org.enterchain.enter.ethereum.vm.operations.MSizeOperation;
import org.enterchain.enter.ethereum.vm.operations.MStore8Operation;
import org.enterchain.enter.ethereum.vm.operations.MStoreOperation;
import org.enterchain.enter.ethereum.vm.operations.ModOperation;
import org.enterchain.enter.ethereum.vm.operations.MulModOperation;
import org.enterchain.enter.ethereum.vm.operations.MulOperation;
import org.enterchain.enter.ethereum.vm.operations.NotOperation;
import org.enterchain.enter.ethereum.vm.operations.NumberOperation;
import org.enterchain.enter.ethereum.vm.operations.OrOperation;
import org.enterchain.enter.ethereum.vm.operations.OriginOperation;
import org.enterchain.enter.ethereum.vm.operations.PCOperation;
import org.enterchain.enter.ethereum.vm.operations.PopOperation;
import org.enterchain.enter.ethereum.vm.operations.PushOperation;
import org.enterchain.enter.ethereum.vm.operations.ReturnDataCopyOperation;
import org.enterchain.enter.ethereum.vm.operations.ReturnDataSizeOperation;
import org.enterchain.enter.ethereum.vm.operations.ReturnOperation;
import org.enterchain.enter.ethereum.vm.operations.RevertOperation;
import org.enterchain.enter.ethereum.vm.operations.SDivOperation;
import org.enterchain.enter.ethereum.vm.operations.SGtOperation;
import org.enterchain.enter.ethereum.vm.operations.SLoadOperation;
import org.enterchain.enter.ethereum.vm.operations.SLtOperation;
import org.enterchain.enter.ethereum.vm.operations.SModOperation;
import org.enterchain.enter.ethereum.vm.operations.SStoreOperation;
import org.enterchain.enter.ethereum.vm.operations.SarOperation;
import org.enterchain.enter.ethereum.vm.operations.SelfBalanceOperation;
import org.enterchain.enter.ethereum.vm.operations.SelfDestructOperation;
import org.enterchain.enter.ethereum.vm.operations.Sha3Operation;
import org.enterchain.enter.ethereum.vm.operations.ShlOperation;
import org.enterchain.enter.ethereum.vm.operations.ShrOperation;
import org.enterchain.enter.ethereum.vm.operations.SignExtendOperation;
import org.enterchain.enter.ethereum.vm.operations.StaticCallOperation;
import org.enterchain.enter.ethereum.vm.operations.StopOperation;
import org.enterchain.enter.ethereum.vm.operations.SubOperation;
import org.enterchain.enter.ethereum.vm.operations.SwapOperation;
import org.enterchain.enter.ethereum.vm.operations.TimestampOperation;
import org.enterchain.enter.ethereum.vm.operations.XorOperation;

import java.math.BigInteger;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

/** Provides EVMs supporting the appropriate operations for mainnet hard forks. */
abstract class MainnetEvmRegistries {

  static EVM frontier(final GasCalculator gasCalculator) {
    final OperationRegistry registry = new OperationRegistry();

    registerFrontierOpcodes(registry, gasCalculator, Account.DEFAULT_VERSION);

    return new EVM(registry, gasCalculator);
  }

  static EVM homestead(final GasCalculator gasCalculator) {
    final OperationRegistry registry = new OperationRegistry();

    registerHomesteadOpcodes(registry, gasCalculator, Account.DEFAULT_VERSION);

    return new EVM(registry, gasCalculator);
  }

  static EVM byzantium(final GasCalculator gasCalculator) {
    final OperationRegistry registry = new OperationRegistry();

    registerByzantiumOpcodes(registry, gasCalculator, Account.DEFAULT_VERSION);

    return new EVM(registry, gasCalculator);
  }

  static EVM constantinople(final GasCalculator gasCalculator) {
    final OperationRegistry registry = new OperationRegistry();

    registerConstantinopleOpcodes(registry, gasCalculator, Account.DEFAULT_VERSION);

    return new EVM(registry, gasCalculator);
  }

  static EVM istanbul(final GasCalculator gasCalculator, final BigInteger chainId) {
    final OperationRegistry registry = new OperationRegistry();

    registerIstanbulOpcodes(registry, gasCalculator, Account.DEFAULT_VERSION, chainId);

    return new EVM(registry, gasCalculator);
  }

  static EVM london(final GasCalculator gasCalculator, final BigInteger chainId) {
    final OperationRegistry registry = new OperationRegistry();

    registerLondonOpcodes(registry, gasCalculator, Account.DEFAULT_VERSION, chainId);

    return new EVM(registry, gasCalculator);
  }

  private static void registerFrontierOpcodes(
      final OperationRegistry registry,
      final GasCalculator gasCalculator,
      final int accountVersion) {
    registry.put(new AddOperation(gasCalculator), accountVersion);
    registry.put(new AddOperation(gasCalculator), accountVersion);
    registry.put(new MulOperation(gasCalculator), accountVersion);
    registry.put(new SubOperation(gasCalculator), accountVersion);
    registry.put(new DivOperation(gasCalculator), accountVersion);
    registry.put(new SDivOperation(gasCalculator), accountVersion);
    registry.put(new ModOperation(gasCalculator), accountVersion);
    registry.put(new SModOperation(gasCalculator), accountVersion);
    registry.put(new ExpOperation(gasCalculator), accountVersion);
    registry.put(new AddModOperation(gasCalculator), accountVersion);
    registry.put(new MulModOperation(gasCalculator), accountVersion);
    registry.put(new SignExtendOperation(gasCalculator), accountVersion);
    registry.put(new LtOperation(gasCalculator), accountVersion);
    registry.put(new GtOperation(gasCalculator), accountVersion);
    registry.put(new SLtOperation(gasCalculator), accountVersion);
    registry.put(new SGtOperation(gasCalculator), accountVersion);
    registry.put(new EqOperation(gasCalculator), accountVersion);
    registry.put(new IsZeroOperation(gasCalculator), accountVersion);
    registry.put(new AndOperation(gasCalculator), accountVersion);
    registry.put(new OrOperation(gasCalculator), accountVersion);
    registry.put(new XorOperation(gasCalculator), accountVersion);
    registry.put(new NotOperation(gasCalculator), accountVersion);
    registry.put(new ByteOperation(gasCalculator), accountVersion);
    registry.put(new Sha3Operation(gasCalculator), accountVersion);
    registry.put(new AddressOperation(gasCalculator), accountVersion);
    registry.put(new BalanceOperation(gasCalculator), accountVersion);
    registry.put(new OriginOperation(gasCalculator), accountVersion);
    registry.put(new CallerOperation(gasCalculator), accountVersion);
    registry.put(new CallValueOperation(gasCalculator), accountVersion);
    registry.put(new CallDataLoadOperation(gasCalculator), accountVersion);
    registry.put(new CallDataSizeOperation(gasCalculator), accountVersion);
    registry.put(new CallDataCopyOperation(gasCalculator), accountVersion);
    registry.put(new CodeSizeOperation(gasCalculator), accountVersion);
    registry.put(new CodeCopyOperation(gasCalculator), accountVersion);
    registry.put(new GasPriceOperation(gasCalculator), accountVersion);
    registry.put(new ExtCodeCopyOperation(gasCalculator), accountVersion);
    registry.put(new ExtCodeSizeOperation(gasCalculator), accountVersion);
    registry.put(new BlockHashOperation(gasCalculator), accountVersion);
    registry.put(new CoinbaseOperation(gasCalculator), accountVersion);
    registry.put(new TimestampOperation(gasCalculator), accountVersion);
    registry.put(new NumberOperation(gasCalculator), accountVersion);
    registry.put(new DifficultyOperation(gasCalculator), accountVersion);
    registry.put(new GasLimitOperation(gasCalculator), accountVersion);
    registry.put(new PopOperation(gasCalculator), accountVersion);
    registry.put(new MLoadOperation(gasCalculator), accountVersion);
    registry.put(new MStoreOperation(gasCalculator), accountVersion);
    registry.put(new MStore8Operation(gasCalculator), accountVersion);
    registry.put(new SLoadOperation(gasCalculator), accountVersion);
    registry.put(
        new SStoreOperation(gasCalculator, SStoreOperation.FRONTIER_MINIMUM), accountVersion);
    registry.put(new JumpOperation(gasCalculator), accountVersion);
    registry.put(new JumpiOperation(gasCalculator), accountVersion);
    registry.put(new PCOperation(gasCalculator), accountVersion);
    registry.put(new MSizeOperation(gasCalculator), accountVersion);
    registry.put(new GasOperation(gasCalculator), accountVersion);
    registry.put(new JumpDestOperation(gasCalculator), accountVersion);
    registry.put(new ReturnOperation(gasCalculator), accountVersion);
    registry.put(new InvalidOperation(gasCalculator), accountVersion);
    registry.put(new StopOperation(gasCalculator), accountVersion);
    registry.put(new SelfDestructOperation(gasCalculator), accountVersion);
    registry.put(new CreateOperation(gasCalculator), accountVersion);
    registry.put(new CallOperation(gasCalculator), accountVersion);
    registry.put(new CallCodeOperation(gasCalculator), accountVersion);

    // Register the PUSH1, PUSH2, ..., PUSH32 operations.
    for (int i = 1; i <= 32; ++i) {
      registry.put(new PushOperation(i, gasCalculator), accountVersion);
    }

    // Register the DUP1, DUP2, ..., DUP16 operations.
    for (int i = 1; i <= 16; ++i) {
      registry.put(new DupOperation(i, gasCalculator), accountVersion);
    }

    // Register the SWAP1, SWAP2, ..., SWAP16 operations.
    for (int i = 1; i <= 16; ++i) {
      registry.put(new SwapOperation(i, gasCalculator), accountVersion);
    }

    // Register the LOG0, LOG1, ..., LOG4 operations.
    for (int i = 0; i < 5; ++i) {
      registry.put(new LogOperation(i, gasCalculator), accountVersion);
    }
  }

  private static void registerHomesteadOpcodes(
      final OperationRegistry registry,
      final GasCalculator gasCalculator,
      final int accountVersion) {
    registerFrontierOpcodes(registry, gasCalculator, accountVersion);
    registry.put(new DelegateCallOperation(gasCalculator), accountVersion);
  }

  private static void registerByzantiumOpcodes(
      final OperationRegistry registry,
      final GasCalculator gasCalculator,
      final int accountVersion) {
    registerHomesteadOpcodes(registry, gasCalculator, accountVersion);
    registry.put(new ReturnDataCopyOperation(gasCalculator), accountVersion);
    registry.put(new ReturnDataSizeOperation(gasCalculator), accountVersion);
    registry.put(new RevertOperation(gasCalculator), accountVersion);
    registry.put(new StaticCallOperation(gasCalculator), accountVersion);
  }

  private static void registerConstantinopleOpcodes(
      final OperationRegistry registry,
      final GasCalculator gasCalculator,
      final int accountVersion) {
    registerByzantiumOpcodes(registry, gasCalculator, accountVersion);
    registry.put(new Create2Operation(gasCalculator), accountVersion);
    registry.put(new SarOperation(gasCalculator), accountVersion);
    registry.put(new ShlOperation(gasCalculator), accountVersion);
    registry.put(new ShrOperation(gasCalculator), accountVersion);
    registry.put(new ExtCodeHashOperation(gasCalculator), accountVersion);
  }

  private static void registerIstanbulOpcodes(
      final OperationRegistry registry,
      final GasCalculator gasCalculator,
      final int accountVersion,
      final BigInteger chainId) {
    registerConstantinopleOpcodes(registry, gasCalculator, accountVersion);
    registry.put(
        new ChainIdOperation(gasCalculator, Bytes32.leftPad(Bytes.of(chainId.toByteArray()))),
        Account.DEFAULT_VERSION);
    registry.put(new SelfBalanceOperation(gasCalculator), Account.DEFAULT_VERSION);
    registry.put(
        new SStoreOperation(gasCalculator, SStoreOperation.EIP_1706_MINIMUM),
        Account.DEFAULT_VERSION);
  }

  private static void registerLondonOpcodes(
      final OperationRegistry registry,
      final GasCalculator gasCalculator,
      final int accountVersion,
      final BigInteger chainId) {
    registerIstanbulOpcodes(registry, gasCalculator, accountVersion, chainId);
    registry.put(new BaseFeeOperation(gasCalculator), Account.DEFAULT_VERSION);
  }
}
