/*
 * Copyright 2020 ConsenSys AG.
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
package tech.pegasys.ethsigner.tests.tls.support.client;

import java.nio.file.Path;
import java.util.Optional;

import tech.pegasys.ethsigner.core.config.KeyStoreOptions;
import tech.pegasys.ethsigner.core.config.tls.client.ClientTlsOptions;

public class BasicClientTlsOptions implements ClientTlsOptions {
  private final Optional<KeyStoreOptions> tlsCertificateOptions;
  private final Optional<Path> knownServersFile;
  private final boolean caAuthEnabled;

  public BasicClientTlsOptions(
      final KeyStoreOptions tlsCertificateOptions,
      final Optional<Path> knownServersFile,
      final boolean caAuthEnabled) {
    this.tlsCertificateOptions = Optional.ofNullable(tlsCertificateOptions);
    this.knownServersFile = knownServersFile;
    this.caAuthEnabled = caAuthEnabled;
  }

  @Override
  public Optional<KeyStoreOptions> getKeyStoreOptions() {
    return tlsCertificateOptions;
  }

  @Override
  public Optional<Path> getKnownServersFile() {
    return knownServersFile;
  }

  @Override
  public boolean isCaAuthEnabled() {
    return caAuthEnabled;
  }
}
