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
package org.enterchain.enter.ethereum.api.jsonrpc.timeout;

import static org.enterchain.enter.ethereum.api.jsonrpc.RpcMethod.ETH_BLOCK_NUMBER;
import static org.enterchain.enter.ethereum.api.jsonrpc.RpcMethod.ETH_GET_LOGS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.enterchain.enter.ethereum.api.handlers.TimeoutHandler;
import org.enterchain.enter.ethereum.api.handlers.TimeoutOptions;
import org.enterchain.enter.ethereum.api.jsonrpc.RpcMethod;
import org.enterchain.enter.ethereum.api.jsonrpc.context.ContextKey;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.JsonRpcRequest;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

@RunWith(Parameterized.class)
public class TimeoutHandlerTest {

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {
            Optional.empty(),
            ETH_GET_LOGS,
            body(ETH_GET_LOGS),
            DEFAULT_OPTS.getTimeoutSeconds(),
            true
          },
          {
            Optional.empty(),
            ETH_GET_LOGS,
            body(ETH_BLOCK_NUMBER),
            DEFAULT_OPTS.getTimeoutSeconds(),
            false
          },
          {
            Optional.of(DEFAULT_OPTS),
            ETH_GET_LOGS,
            body(ETH_BLOCK_NUMBER),
            DEFAULT_OPTS.getTimeoutSeconds(),
            true
          }
        });
  }

  private static final TimeoutOptions DEFAULT_OPTS = TimeoutOptions.defaultOptions();
  private final Optional<TimeoutOptions> globalOptions;
  private final String body;
  private final RpcMethod method;
  private final long timeoutSec;
  private final boolean timerMustBeSet;

  public TimeoutHandlerTest(
      final Optional<TimeoutOptions> globalOptions,
      final RpcMethod method,
      final String body,
      final long timeoutSec,
      final boolean timerMustBeSet) {
    this.globalOptions = globalOptions;
    this.body = body;
    this.method = method;
    this.timeoutSec = timeoutSec;
    this.timerMustBeSet = timerMustBeSet;
  }

  @Test
  public void test() {
    final Map<String, TimeoutOptions> options =
        ImmutableMap.of(
            method.getMethodName(), new TimeoutOptions(timeoutSec, DEFAULT_OPTS.getErrorCode()));
    final Handler<RoutingContext> handler = TimeoutHandler.handler(globalOptions, options, true);
    final RoutingContext ctx = Mockito.spy(RoutingContext.class);
    final Vertx vertx = Mockito.spy(Vertx.class);
    when(ctx.getBodyAsString()).thenReturn(body);
    when(ctx.vertx()).thenReturn(vertx);
    handler.handle(ctx);
    verify(ctx).put(eq(ContextKey.REQUEST_BODY_AS_JSON_OBJECT.name()), any());
    verify(vertx, times(timerMustBeSet ? 1 : 0))
        .setTimer(eq(TimeUnit.SECONDS.toMillis(timeoutSec)), any());
    verify(ctx, times(timerMustBeSet ? 1 : 0)).addBodyEndHandler(any());
  }

  private static String body(final RpcMethod method) {
    return Json.encodePrettily(new JsonRpcRequest("2.0", method.getMethodName(), null));
  }
}
