/*
 * Copyright 2018 ConsenSys AG.
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
package tech.pegasys.ethsigner.core.jsonrpc;

import java.math.BigInteger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import tech.pegasys.ethsigner.core.jsonrpc.exception.InvalidJsonRpcRequestException;

public class JsonRpcRequestId {

  private static final Class<?>[] VALID_ID_TYPES =
      new Class<?>[] {
        String.class, Integer.class, Long.class, Float.class, Double.class, BigInteger.class
      };

  private final Object id;

  @JsonCreator
  public JsonRpcRequestId(final Object id) {
    if (isRequestTypeInvalid(id)) {
      throw new InvalidJsonRpcRequestException("Invalid id");
    }
    this.id = id;
  }

  @JsonValue
  public Object getValue() {
    return id;
  }

  private boolean isRequestTypeInvalid(final Object id) {
    return isNotNull(id) && isTypeInvalid(id);
  }

  /**
   * The JSON spec says "The use of Null as a value for the id member in a Request object is
   * discouraged" Both geth and parity accept null values, so we decided to support them as well.
   */
  private boolean isNotNull(final Object id) {
    return id != null;
  }

  private boolean isTypeInvalid(final Object id) {
    for (final Class<?> validType : VALID_ID_TYPES) {
      if (validType.isInstance(id)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final JsonRpcRequestId that = (JsonRpcRequestId) o;
    return Objects.equal(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("id", id).toString();
  }
}
