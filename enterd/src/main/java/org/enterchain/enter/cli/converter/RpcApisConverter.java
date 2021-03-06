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
package org.enterchain.enter.cli.converter;

import org.enterchain.enter.cli.converter.exception.RpcApisConversionException;
import org.enterchain.enter.consensus.clique.jsonrpc.CliqueRpcApis;
import org.enterchain.enter.consensus.ibft.jsonrpc.IbftRpcApis;
import org.enterchain.enter.consensus.qbft.jsonrpc.QbftRpcApis;
import org.enterchain.enter.ethereum.api.jsonrpc.RpcApi;
import org.enterchain.enter.ethereum.api.jsonrpc.RpcApis;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import picocli.CommandLine;

public class RpcApisConverter implements CommandLine.ITypeConverter<RpcApi> {

  @Override
  public RpcApi convert(final String name) throws RpcApisConversionException {
    return Stream.<Function<String, Optional<RpcApi>>>of(
            RpcApis::valueOf, CliqueRpcApis::valueOf, IbftRpcApis::valueOf, QbftRpcApis::valueOf)
        .map(f -> f.apply(name.trim().toUpperCase()))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst()
        .orElseThrow(() -> new RpcApisConversionException(name));
  }
}
