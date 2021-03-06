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
package org.enterchain.enter.ethereum.api.jsonrpc;

import org.enterchain.enter.ethereum.api.handlers.TimeoutOptions;
import org.enterchain.enter.ethereum.api.tls.TlsConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.common.base.MoreObjects;

public class JsonRpcConfiguration {
  private static final String DEFAULT_JSON_RPC_HOST = "127.0.0.1";
  public static final int DEFAULT_JSON_RPC_PORT = 8545;
  public static final int DEFAULT_MAX_ACTIVE_CONNECTIONS = 80;

  private boolean enabled;
  private int port;
  private String host;
  private List<String> corsAllowedDomains = Collections.emptyList();
  private List<RpcApi> rpcApis;
  private List<String> hostsAllowlist = Arrays.asList("localhost", "127.0.0.1");
  private boolean authenticationEnabled = false;
  private String authenticationCredentialsFile;
  private File authenticationPublicKeyFile;
  private Optional<TlsConfiguration> tlsConfiguration = Optional.empty();
  private long httpTimeoutSec = TimeoutOptions.defaultOptions().getTimeoutSeconds();
  private int maxActiveConnections;

  public static JsonRpcConfiguration createDefault() {
    final JsonRpcConfiguration config = new JsonRpcConfiguration();
    config.setEnabled(false);
    config.setPort(DEFAULT_JSON_RPC_PORT);
    config.setHost(DEFAULT_JSON_RPC_HOST);
    config.rpcApis = RpcApis.DEFAULT_JSON_RPC_APIS;
    config.httpTimeoutSec = TimeoutOptions.defaultOptions().getTimeoutSeconds();
    config.setMaxActiveConnections(DEFAULT_MAX_ACTIVE_CONNECTIONS);
    return config;
  }

  private JsonRpcConfiguration() {}

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public int getPort() {
    return port;
  }

  public void setPort(final int port) {
    this.port = port;
  }

  public String getHost() {
    return host;
  }

  public void setHost(final String host) {
    this.host = host;
  }

  public Collection<String> getCorsAllowedDomains() {
    return corsAllowedDomains;
  }

  public void setCorsAllowedDomains(final List<String> corsAllowedDomains) {
    if (corsAllowedDomains != null) {
      this.corsAllowedDomains = corsAllowedDomains;
    }
  }

  public Collection<RpcApi> getRpcApis() {
    return rpcApis;
  }

  public void setRpcApis(final List<RpcApi> rpcApis) {
    this.rpcApis = rpcApis;
  }

  public void addRpcApi(final RpcApi rpcApi) {
    this.rpcApis = new ArrayList<>(rpcApis);
    rpcApis.add(rpcApi);
  }

  public Collection<String> getHostsAllowlist() {
    return Collections.unmodifiableCollection(this.hostsAllowlist);
  }

  public void setHostsAllowlist(final List<String> hostsWhitelist) {
    this.hostsAllowlist = hostsWhitelist;
  }

  public boolean isAuthenticationEnabled() {
    return authenticationEnabled;
  }

  public void setAuthenticationEnabled(final boolean authenticationEnabled) {
    this.authenticationEnabled = authenticationEnabled;
  }

  public void setAuthenticationCredentialsFile(final String authenticationCredentialsFile) {
    this.authenticationCredentialsFile = authenticationCredentialsFile;
  }

  public String getAuthenticationCredentialsFile() {
    return authenticationCredentialsFile;
  }

  public File getAuthenticationPublicKeyFile() {
    return authenticationPublicKeyFile;
  }

  public void setAuthenticationPublicKeyFile(final File authenticationPublicKeyFile) {
    this.authenticationPublicKeyFile = authenticationPublicKeyFile;
  }

  public Optional<TlsConfiguration> getTlsConfiguration() {
    return tlsConfiguration;
  }

  public void setTlsConfiguration(final Optional<TlsConfiguration> tlsConfiguration) {
    this.tlsConfiguration = tlsConfiguration;
  }

  public long getHttpTimeoutSec() {
    return httpTimeoutSec;
  }

  public void setHttpTimeoutSec(final long httpTimeoutSec) {
    this.httpTimeoutSec = httpTimeoutSec;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("enabled", enabled)
        .add("port", port)
        .add("host", host)
        .add("corsAllowedDomains", corsAllowedDomains)
        .add("hostsAllowlist", hostsAllowlist)
        .add("rpcApis", rpcApis)
        .add("authenticationEnabled", authenticationEnabled)
        .add("authenticationCredentialsFile", authenticationCredentialsFile)
        .add("authenticationPublicKeyFile", authenticationPublicKeyFile)
        .add("tlsConfiguration", tlsConfiguration)
        .add("httpTimeoutSec", httpTimeoutSec)
        .add("maxActiveConnections", maxActiveConnections)
        .toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final JsonRpcConfiguration that = (JsonRpcConfiguration) o;
    return enabled == that.enabled
        && port == that.port
        && authenticationEnabled == that.authenticationEnabled
        && Objects.equals(host, that.host)
        && Objects.equals(corsAllowedDomains, that.corsAllowedDomains)
        && Objects.equals(rpcApis, that.rpcApis)
        && Objects.equals(hostsAllowlist, that.hostsAllowlist)
        && Objects.equals(authenticationCredentialsFile, that.authenticationCredentialsFile)
        && Objects.equals(authenticationPublicKeyFile, that.authenticationPublicKeyFile);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        enabled,
        port,
        host,
        corsAllowedDomains,
        rpcApis,
        hostsAllowlist,
        authenticationEnabled,
        authenticationCredentialsFile,
        authenticationPublicKeyFile);
  }

  public int getMaxActiveConnections() {
    return maxActiveConnections;
  }

  public void setMaxActiveConnections(final int maxActiveConnections) {
    this.maxActiveConnections = maxActiveConnections;
  }
}
