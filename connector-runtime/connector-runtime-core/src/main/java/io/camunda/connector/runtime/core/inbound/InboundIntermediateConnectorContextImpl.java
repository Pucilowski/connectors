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
package io.camunda.connector.runtime.core.inbound;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.connector.api.inbound.Health;
import io.camunda.connector.api.inbound.InboundConnectorContext;
import io.camunda.connector.api.inbound.InboundConnectorDefinition;
import io.camunda.connector.api.inbound.InboundIntermediateConnectorContext;
import io.camunda.connector.api.inbound.ProcessInstanceContext;
import io.camunda.connector.api.validation.ValidationProvider;
import io.camunda.connector.runtime.core.inbound.correlation.BoundaryEventCorrelationPoint;
import io.camunda.connector.runtime.core.inbound.correlation.InboundCorrelationHandler;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Implementation of {@link InboundIntermediateConnectorContext} that extends {@link
 * InboundConnectorContext} and enables runtime updates of context properties from Operate.
 */
public class InboundIntermediateConnectorContextImpl
    implements InboundIntermediateConnectorContext {
  private final InboundConnectorContext inboundContext;
  private final OperateClientAdapter operateClient;
  private final ValidationProvider validationProvider;
  private final ObjectMapper objectMapper;
  private final InboundCorrelationHandler correlationHandler;

  public InboundIntermediateConnectorContextImpl(
      final InboundConnectorContext inboundContext,
      final OperateClientAdapter operateClient,
      final ValidationProvider validationProvider,
      final ObjectMapper objectMapper,
      final InboundCorrelationHandler correlationHandler) {
    this.inboundContext = inboundContext;
    this.operateClient = operateClient;
    this.validationProvider = validationProvider;
    this.objectMapper = objectMapper;
    this.correlationHandler = correlationHandler;
  }

  @Override
  public List<ProcessInstanceContext> getProcessInstanceContexts() {
    var elementId = getDefinition().elementId();
    var definition = (InboundConnectorDefinitionImpl) getDefinition();
    if (definition.correlationPoint() instanceof BoundaryEventCorrelationPoint point) {
      elementId = point.attachedTo().elementId();
    }

    List<FlowNodeInstance> activeProcessInstanceKeys =
        operateClient.fetchActiveProcessInstanceKeyByDefinitionKeyAndElementId(
            getDefinition().processDefinitionKey(), elementId);

    return activeProcessInstanceKeys.stream()
        .map(this::createProcessInstanceContext)
        .collect(Collectors.toList());
  }

  private ProcessInstanceContext createProcessInstanceContext(FlowNodeInstance node) {
    Supplier<Map<String, Object>> variableSupplier =
        () -> operateClient.fetchVariablesByProcessInstanceKey(node.processInstanceKey());

    return new DefaultProcessInstanceContext(
        this, node, validationProvider, correlationHandler, objectMapper, variableSupplier);
  }

  @Override
  public void correlate(final Object variables) {
    inboundContext.correlate(variables);
  }

  @Override
  public void cancel(final Throwable exception) {
    inboundContext.cancel(exception);
  }

  @Override
  public Map<String, Object> getProperties() {
    return inboundContext.getProperties();
  }

  @Override
  public <T> T bindProperties(final Class<T> cls) {
    return inboundContext.bindProperties(cls);
  }

  @Override
  public InboundConnectorDefinition getDefinition() {
    return inboundContext.getDefinition();
  }

  @Override
  public void reportHealth(final Health health) {
    inboundContext.reportHealth(health);
  }
}
