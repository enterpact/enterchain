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
package tech.pegasys.ethsigner.core.jsonrpc;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.base.Objects;
import tech.pegasys.ethsigner.core.jsonrpc.exception.InvalidJsonRpcRequestException;

@JsonPropertyOrder({"jsonrpc", "method", "params", "id"})
public class JsonRpcRequest {

  private final String method;
  private final String version;
  private JsonRpcRequestId id;
  private Object params;

  @JsonCreator
  public JsonRpcRequest(
      @JsonProperty("jsonrpc") final String version, @JsonProperty("method") final String method) {
    this.version = version;
    this.method = method;
    if (method == null) {
      throw new InvalidJsonRpcRequestException("Field 'method' is required");
    }
  }

  @JsonGetter("id")
  public JsonRpcRequestId getId() {
    return id;
  }

  @JsonGetter("method")
  public String getMethod() {
    return method;
  }

  @JsonGetter("jsonrpc")
  public String getVersion() {
    return version;
  }

  @JsonInclude(Include.NON_NULL)
  @JsonGetter("params")
  public Object getParams() {
    return params;
  }

  @JsonSetter("id")
  public void setId(final JsonRpcRequestId id) {
    this.id = id;
  }

  @JsonSetter("params")
  public void setParams(final Object params) {
    this.params = params;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final JsonRpcRequest that = (JsonRpcRequest) o;

    return isParamsEqual(that.params)
        && Objects.equal(id, that.id)
        && Objects.equal(method, that.method)
        && Objects.equal(version, that.version);
  }

  private boolean isParamsEqual(final Object otherParams) {
    if (params.getClass().isArray()) {
      if (!otherParams.getClass().isArray()) {
        return false;
      }
      final Object[] paramsArray = (Object[]) params;
      final Object[] thatParamsArray = (Object[]) otherParams;
      return Arrays.equals(paramsArray, thatParamsArray);
    } else if (otherParams.getClass().isArray()) {
      return false;
    }

    return params.equals(otherParams);
  }

  @Override
  public int hashCode() {
    final int paramsHashCode;
    if (params.getClass().isArray()) {
      paramsHashCode = Arrays.hashCode((Object[]) params);
    } else {
      paramsHashCode = params.hashCode();
    }
    return Objects.hashCode(id, method, paramsHashCode, version);
  }
}
