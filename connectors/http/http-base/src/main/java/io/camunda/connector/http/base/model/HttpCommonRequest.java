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
package io.camunda.connector.http.base.model;

import io.camunda.connector.feel.annotation.FEEL;
import io.camunda.connector.generator.dsl.Property.FeelMode;
import io.camunda.connector.generator.java.annotation.TemplateProperty;
import io.camunda.connector.generator.java.annotation.TemplateProperty.PropertyCondition;
import io.camunda.connector.generator.java.annotation.TemplateProperty.PropertyType;
import io.camunda.connector.http.base.auth.Authentication;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Map;
import java.util.Objects;

public class HttpCommonRequest {

  @FEEL
  @NotNull
  @TemplateProperty(group = "endpoint", id = "method", defaultValue = "GET")
  private HttpMethod method;

  @FEEL
  @NotBlank
  @Pattern(regexp = "^(=|http://|https://|secrets|\\{\\{).*$", message = "Must be a http(s) URL")
  @TemplateProperty(group = "endpoint", label = "URL")
  private String url;

  @Valid private Authentication authentication;

  @TemplateProperty(
      group = "timeout",
      defaultValue = "20",
      optional = true,
      description =
          "Sets the timeout in seconds to establish a connection or 0 for an infinite timeout")
  private Integer connectionTimeoutInSeconds;

  @FEEL
  @TemplateProperty(
      feel = FeelMode.required,
      group = "endpoint",
      optional = true,
      description = "Map of HTTP headers to add to the request")
  private Map<String, String> headers;

  @FEEL
  @TemplateProperty(
      label = "Request body",
      description = "Payload to send with the request",
      feel = FeelMode.optional,
      group = "payload",
      type = PropertyType.Text,
      optional = true,
      condition =
          @PropertyCondition(
              property = "method",
              oneOf = {"POST", "PUT", "PATCH"}))
  private Object body;

  @FEEL
  @TemplateProperty(
      feel = FeelMode.required,
      group = "endpoint",
      optional = true,
      description = "Map of query parameters to add to the request URL")
  private Map<String, String> queryParameters;

  public Object getBody() {
    return body;
  }

  public void setBody(final Object body) {
    this.body = body;
  }

  public boolean hasHeaders() {
    return headers != null;
  }

  public boolean hasBody() {
    return body != null;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(final Map<String, String> headers) {
    this.headers = headers;
  }

  public boolean hasQueryParameters() {
    return queryParameters != null;
  }

  public Map<String, String> getQueryParameters() {
    return queryParameters;
  }

  public void setQueryParameters(Map<String, String> queryParameters) {
    this.queryParameters = queryParameters;
  }

  public boolean hasAuthentication() {
    return authentication != null;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Authentication getAuthentication() {
    return authentication;
  }

  public void setAuthentication(Authentication authentication) {
    this.authentication = authentication;
  }

  public HttpMethod getMethod() {
    return method;
  }

  public void setMethod(HttpMethod method) {
    this.method = method;
  }

  public Integer getConnectionTimeoutInSeconds() {
    return connectionTimeoutInSeconds;
  }

  public void setConnectionTimeoutInSeconds(Integer connectionTimeoutInSeconds) {
    this.connectionTimeoutInSeconds = connectionTimeoutInSeconds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HttpCommonRequest that = (HttpCommonRequest) o;
    return url.equals(that.url)
        && method.equals(that.method)
        && Objects.equals(authentication, that.authentication)
        && Objects.equals(connectionTimeoutInSeconds, that.connectionTimeoutInSeconds)
        && Objects.equals(headers, that.headers)
        && Objects.equals(body, that.body)
        && Objects.equals(queryParameters, that.queryParameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        url, method, authentication, connectionTimeoutInSeconds, headers, body, queryParameters);
  }

  @Override
  public String toString() {
    return "HttpRequest{"
        + "url='"
        + url
        + '\''
        + ", method='"
        + method
        + '\''
        + ", authentication="
        + authentication
        + ", connectionTimeoutInSeconds='"
        + connectionTimeoutInSeconds
        + '\''
        + ", headers="
        + headers
        + ", body="
        + body
        + ", queryParameters="
        + queryParameters
        + '}';
  }
}
