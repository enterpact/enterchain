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
package org.enterchain.enter.ethereum.api.jsonrpc.websocket.methods;

import static org.assertj.core.api.Assertions.assertThat;

import org.enterchain.enter.ethereum.api.handlers.TimeoutOptions;
import org.enterchain.enter.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.enterchain.enter.ethereum.api.jsonrpc.websocket.WebSocketRequestHandler;
import org.enterchain.enter.ethereum.api.jsonrpc.websocket.subscription.Subscription;
import org.enterchain.enter.ethereum.api.jsonrpc.websocket.subscription.SubscriptionManager;
import org.enterchain.enter.ethereum.api.jsonrpc.websocket.subscription.request.SubscriptionType;
import org.enterchain.enter.ethereum.api.jsonrpc.websocket.subscription.syncing.SyncingSubscription;
import org.enterchain.enter.ethereum.eth.manager.EthScheduler;
import org.enterchain.enter.metrics.noop.NoOpMetricsSystem;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(VertxUnitRunner.class)
public class EthSubscribeIntegrationTest {

  private Vertx vertx;
  private WebSocketRequestHandler webSocketRequestHandler;
  private SubscriptionManager subscriptionManager;
  private WebSocketMethodsFactory webSocketMethodsFactory;
  private final int ASYNC_TIMEOUT = 5000;
  private final String CONNECTION_ID_1 = "test-connection-id-1";
  private final String CONNECTION_ID_2 = "test-connection-id-2";

  @Before
  public void before() {
    vertx = Vertx.vertx();
    subscriptionManager = new SubscriptionManager(new NoOpMetricsSystem());
    webSocketMethodsFactory = new WebSocketMethodsFactory(subscriptionManager, new HashMap<>());
    webSocketRequestHandler =
        new WebSocketRequestHandler(
            vertx,
            webSocketMethodsFactory.methods(),
            Mockito.mock(EthScheduler.class),
            TimeoutOptions.defaultOptions().getTimeoutSeconds());
  }

  @Test
  public void shouldAddConnectionToMap(final TestContext context) {
    final Async async = context.async();

    final JsonRpcRequest subscribeRequestBody = createEthSubscribeRequestBody(CONNECTION_ID_1);

    vertx
        .eventBus()
        .consumer(CONNECTION_ID_1)
        .handler(
            msg -> {
              final List<SyncingSubscription> syncingSubscriptions = getSubscriptions();
              assertThat(syncingSubscriptions).hasSize(1);
              Assertions.assertThat(syncingSubscriptions.get(0).getConnectionId())
                  .isEqualTo(CONNECTION_ID_1);
              async.complete();
            })
        .completionHandler(
            v ->
                webSocketRequestHandler.handle(CONNECTION_ID_1, Json.encode(subscribeRequestBody)));

    async.awaitSuccess(ASYNC_TIMEOUT);
  }

  @Test
  public void shouldAddMultipleConnectionsToMap(final TestContext context) {
    final Async async = context.async(2);

    final JsonRpcRequest subscribeRequestBody1 = createEthSubscribeRequestBody(CONNECTION_ID_1);
    final JsonRpcRequest subscribeRequestBody2 = createEthSubscribeRequestBody(CONNECTION_ID_2);

    vertx
        .eventBus()
        .consumer(CONNECTION_ID_1)
        .handler(
            msg -> {
              final List<SyncingSubscription> subscriptions = getSubscriptions();
              assertThat(subscriptions).hasSize(1);
              Assertions.assertThat(subscriptions.get(0).getConnectionId())
                  .isEqualTo(CONNECTION_ID_1);
              async.countDown();

              vertx
                  .eventBus()
                  .consumer(CONNECTION_ID_2)
                  .handler(
                      msg2 -> {
                        final List<SyncingSubscription> updatedSubscriptions = getSubscriptions();
                        assertThat(updatedSubscriptions).hasSize(2);
                        final List<String> connectionIds =
                            updatedSubscriptions.stream()
                                .map(Subscription::getConnectionId)
                                .collect(Collectors.toList());
                        assertThat(connectionIds)
                            .containsExactlyInAnyOrder(CONNECTION_ID_1, CONNECTION_ID_2);
                        async.countDown();
                      })
                  .completionHandler(
                      v ->
                          webSocketRequestHandler.handle(
                              CONNECTION_ID_2, Json.encode(subscribeRequestBody2)));
            })
        .completionHandler(
            v ->
                webSocketRequestHandler.handle(
                    CONNECTION_ID_1, Json.encode(subscribeRequestBody1)));

    async.awaitSuccess(ASYNC_TIMEOUT);
  }

  private List<SyncingSubscription> getSubscriptions() {
    return subscriptionManager.subscriptionsOfType(
        SubscriptionType.SYNCING, SyncingSubscription.class);
  }

  private WebSocketRpcRequest createEthSubscribeRequestBody(final String connectionId) {
    return Json.decodeValue(
        "{\"id\": 1, \"method\": \"eth_subscribe\", \"params\": [\"syncing\"], \"connectionId\": \""
            + connectionId
            + "\"}",
        WebSocketRpcRequest.class);
  }
}
