/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.connector.http.base.auth;

import com.google.api.client.http.HttpHeaders;
import com.google.common.base.Objects;
import io.camunda.connector.feel.annotation.FEEL;
import io.camunda.connector.generator.java.annotation.TemplateProperty;
import io.camunda.connector.generator.java.annotation.TemplateSubType;
import jakarta.validation.constraints.NotEmpty;

@TemplateSubType(id = "bearer", label = "Bearer token")
public final class BearerAuthentication extends Authentication {

  @FEEL
  @NotEmpty
  @TemplateProperty(group = "authentication", label = "Bearer token")
  private String token;

  @Override
  public void setHeaders(final HttpHeaders headers) {
    headers.setAuthorization("Bearer " + token);
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    BearerAuthentication that = (BearerAuthentication) o;
    return Objects.equal(token, that.token);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(super.hashCode(), token);
  }

  @Override
  public String toString() {
    return "BearerAuthentication{" + "token='[REDACTED]'" + "}; Super: " + super.toString();
  }

  @TemplateProperty(ignore = true)
  public static final String TYPE = "bearer";
}
