/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.ethsigner.core.requesthandler.sendtransaction;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static tech.pegasys.ethsigner.core.jsonrpc.response.JsonRpcError.INTERNAL_ERROR;

import java.util.Map.Entry;

import io.vertx.ext.web.RoutingContext;
import tech.pegasys.ethsigner.core.jsonrpc.exception.JsonRpcException;
import tech.pegasys.ethsigner.core.requesthandler.VertxRequestTransmitterFactory;
import tech.pegasys.ethsigner.core.requesthandler.sendtransaction.transaction.Transaction;
import tech.pegasys.ethsigner.core.signing.TransactionSerializer;

public class RetryingTransactionTransmitter extends TransactionTransmitter {

  private final RetryMechanism retryMechanism;

  public RetryingTransactionTransmitter(
      final Transaction transaction,
      final TransactionSerializer transactionSerializer,
      final VertxRequestTransmitterFactory transmitterFactory,
      final RetryMechanism retryMechanism,
      final RoutingContext routingContext) {
    super(transaction, transactionSerializer, transmitterFactory, routingContext);
    this.retryMechanism = retryMechanism;
  }

  @Override
  public void handleResponse(
      final Iterable<Entry<String, String>> headers, final int statusCode, final String body) {
    if (retryMechanism.responseRequiresRetry(statusCode, body)) {
      if (retryMechanism.retriesAvailable()) {
        retryMechanism.incrementRetries();
        send();
      } else {
        context().fail(BAD_REQUEST.code(), new JsonRpcException(INTERNAL_ERROR));
      }
      return;
    }

    super.handleResponse(headers, statusCode, body);
  }
}
