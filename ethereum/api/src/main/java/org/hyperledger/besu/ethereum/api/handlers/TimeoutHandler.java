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
package org.enterchain.enter.ethereum.api.handlers;

import static java.util.Collections.emptyMap;

import org.enterchain.enter.ethereum.api.jsonrpc.context.ContextKey;

import java.util.Map;
import java.util.Optional;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class TimeoutHandler {

  public static Handler<RoutingContext> handler(
      final Optional<TimeoutOptions> globalOptions, final boolean decodeJSON) {
    return handler(globalOptions, emptyMap(), decodeJSON);
  }

  public static Handler<RoutingContext> handler(
      final Optional<TimeoutOptions> globalOptions,
      final Map<String, TimeoutOptions> timeoutOptionsByMethod,
      final boolean decodeJSON) {
    assert timeoutOptionsByMethod != null;
    return ctx -> processHandler(ctx, globalOptions, timeoutOptionsByMethod, decodeJSON);
  }

  private static void processHandler(
      final RoutingContext ctx,
      final Optional<TimeoutOptions> globalOptions,
      final Map<String, TimeoutOptions> timeoutOptionsByMethod,
      final boolean decodeJSON) {
    try {
      final String bodyAsString = ctx.getBodyAsString();
      if (bodyAsString != null) {
        final String json = ctx.getBodyAsString().trim();
        Optional<TimeoutOptions> methodTimeoutOptions = Optional.empty();
        if (decodeJSON && !json.isEmpty() && json.charAt(0) == '{') {
          final JsonObject requestBodyJsonObject = new JsonObject(json);
          ctx.put(ContextKey.REQUEST_BODY_AS_JSON_OBJECT.name(), requestBodyJsonObject);
          final String method = requestBodyJsonObject.getString("method");
          methodTimeoutOptions = Optional.ofNullable(timeoutOptionsByMethod.get(method));
        }
        methodTimeoutOptions
            .or(() -> globalOptions)
            .ifPresent(
                timeoutOptions -> {
                  long tid =
                      ctx.vertx()
                          .setTimer(
                              timeoutOptions.getTimeoutMillis(),
                              t -> {
                                ctx.fail(timeoutOptions.getErrorCode());
                                ctx.response().close();
                              });
                  ctx.addBodyEndHandler(v -> ctx.vertx().cancelTimer(tid));
                });
      }
    } finally {
      ctx.next();
    }
  }
}
