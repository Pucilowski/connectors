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
package io.camunda.connector.runtime.inbound.webhook;

import static java.util.Collections.emptyMap;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.inbound.webhook.MappedHttpRequest;
import io.camunda.connector.api.inbound.webhook.WebhookConnectorException;
import io.camunda.connector.api.inbound.webhook.WebhookConnectorException.WebhookSecurityException;
import io.camunda.connector.api.inbound.webhook.WebhookConnectorExecutable;
import io.camunda.connector.api.inbound.webhook.WebhookProcessingPayload;
import io.camunda.connector.api.inbound.webhook.WebhookResult;
import io.camunda.connector.api.inbound.webhook.WebhookResultContext;
import io.camunda.connector.api.inbound.webhook.WebhookTriggerResultContext;
import io.camunda.connector.feel.FeelEngineWrapperException;
import io.camunda.connector.runtime.inbound.lifecycle.ActiveInboundConnector;
import io.camunda.connector.runtime.inbound.webhook.model.HttpServletRequestWebhookProcessingPayload;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InboundWebhookRestController {

  private static final Logger LOG = LoggerFactory.getLogger(InboundWebhookRestController.class);

  private final WebhookConnectorRegistry webhookConnectorRegistry;

  @Autowired
  public InboundWebhookRestController(final WebhookConnectorRegistry webhookConnectorRegistry) {
    this.webhookConnectorRegistry = webhookConnectorRegistry;
  }

  @RequestMapping(
      method = {GET, POST, PUT, DELETE},
      path = "/inbound/{context}")
  public ResponseEntity<?> inbound(
      @PathVariable String context,
      @RequestHeader Map<String, String> headers,
      @RequestBody(required = false) byte[] bodyAsByteArray,
      @RequestParam Map<String, String> params,
      HttpServletRequest httpServletRequest)
      throws IOException {
    LOG.trace("Received inbound hook on {}", context);
    return webhookConnectorRegistry
        .getWebhookConnectorByContextPath(context)
        .map(
            connector -> {
              WebhookProcessingPayload payload =
                  new HttpServletRequestWebhookProcessingPayload(
                      httpServletRequest, params, headers, bodyAsByteArray);
              return processWebhook(connector, payload);
            })
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  private ResponseEntity<?> processWebhook(
      ActiveInboundConnector connector, WebhookProcessingPayload payload) {
    ResponseEntity<?> connectorResponse;
    try {
      var webhookResult =
          ((WebhookConnectorExecutable) connector.executable()).triggerWebhook(payload);
      var ctxData = toWebhookTriggerResultContext(webhookResult);
      connector.context().correlate(ctxData);
      var processVariablesContext = toWebhookResultContext(webhookResult);
      if (webhookResult.response() != null) {
        connectorResponse = ResponseEntity.ok(webhookResult.response().body());
      } else {
        if (webhookResult.responseBodyExpression() != null) {
          var httpResponseData =
              webhookResult.responseBodyExpression().apply(processVariablesContext);
          connectorResponse = ResponseEntity.ok(httpResponseData);
        } else {
          connectorResponse = ResponseEntity.ok().build();
        }
      }
    } catch (Exception e) {
      LOG.info("Webhook: {} failed with exception", connector.context().getDefinition(), e);
      if (e instanceof FeelEngineWrapperException feelEngineWrapperException) {
        var error =
            new FeelExpressionErrorResponse(
                feelEngineWrapperException.getReason(), feelEngineWrapperException.getExpression());
        connectorResponse = ResponseEntity.unprocessableEntity().body(error);
      } else if (e instanceof ConnectorException connectorException) {
        if (e instanceof WebhookConnectorException webhookConnectorException) {
          connectorResponse = handleWebhookConnectorException(webhookConnectorException);
        } else {
          connectorResponse =
              ResponseEntity.unprocessableEntity()
                  .body(
                      new ErrorResponse(
                          connectorException.getErrorCode(), connectorException.getMessage()));
        }
      } else {
        connectorResponse = ResponseEntity.internalServerError().build();
      }
    }
    return connectorResponse;
  }

  // This will be used to correlate data returned from connector.
  // In other words, we pass this data to Zeebe.
  private WebhookTriggerResultContext toWebhookTriggerResultContext(WebhookResult processedResult) {
    if (processedResult == null) {
      return new WebhookTriggerResultContext(null, null);
    }
    return new WebhookTriggerResultContext(
        new MappedHttpRequest(
            Optional.ofNullable(processedResult.request().body()).orElse(emptyMap()),
            Optional.ofNullable(processedResult.request().headers()).orElse(emptyMap()),
            Optional.ofNullable(processedResult.request().params()).orElse(emptyMap())),
        Optional.ofNullable(processedResult.connectorData()).orElse(emptyMap()));
  }

  // This data will be used to compose a response.
  // In other words, depending on the response body expression,
  // this data may be returned to the webhook caller.
  private WebhookResultContext toWebhookResultContext(WebhookResult processedResult) {
    if (processedResult == null) {
      return new WebhookResultContext(null, null, null);
    }
    return new WebhookResultContext(
        new MappedHttpRequest(
            Optional.ofNullable(processedResult.request().body()).orElse(emptyMap()),
            Optional.ofNullable(processedResult.request().headers()).orElse(emptyMap()),
            Optional.ofNullable(processedResult.request().params()).orElse(emptyMap())),
        Optional.ofNullable(processedResult.connectorData()).orElse(emptyMap()),
        Map.of());
  }

  private ResponseEntity<?> handleWebhookConnectorException(WebhookConnectorException e) {
    var status = HttpStatus.valueOf(e.getStatusCode());
    if (e instanceof WebhookSecurityException) {
      LOG.warn("Webhook failed with security-related exception", e);
      // no message will be included for security reasons
      return ResponseEntity.status(status).body(null);
    }
    if (status.is5xxServerError()) {
      LOG.error("Webhook failed with exception", e);
      // no message will be included for security reasons
      return ResponseEntity.status(status).body(null);
    }
    if (status.is4xxClientError()) {
      return ResponseEntity.status(status).body(new GenericErrorResponse(e.getMessage()));
    }
    return ResponseEntity.status(status).build();
  }
}
