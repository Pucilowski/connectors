/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.http.polling.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.camunda.connector.api.inbound.ProcessInstanceContext;
import io.camunda.connector.http.base.model.HttpCommonRequest;
import io.camunda.connector.http.base.model.HttpCommonResult;
import io.camunda.connector.http.base.services.HttpService;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HttpRequestTaskTest {

  @Mock private HttpService mockHttpService;

  @Mock private HttpCommonResult httpCommonResult;

  @Mock private ProcessInstanceContext mockProcessInstanceContext;

  @Test
  public void shouldExecuteAndCorrelateHttpRequestOnRun()
      throws IOException, InstantiationException, IllegalAccessException {
    // Given
    HttpRequestTask task = new HttpRequestTask(mockHttpService, mockProcessInstanceContext);
    when(mockProcessInstanceContext.bind(HttpCommonRequest.class))
        .thenReturn(new HttpCommonRequest());
    when(mockHttpService.executeConnectorRequest(any(HttpCommonRequest.class)))
        .thenReturn(httpCommonResult);

    // When
    task.run();

    // Then
    verify(mockProcessInstanceContext).correlate(httpCommonResult);
  }

  @Test
  public void shouldHandleExceptionWhileExecutingHttpRequest()
      throws IOException, InstantiationException, IllegalAccessException {
    // Given
    HttpRequestTask task = new HttpRequestTask(mockHttpService, mockProcessInstanceContext);
    when(mockProcessInstanceContext.bind(HttpCommonRequest.class))
        .thenReturn(new HttpCommonRequest());
    when(mockHttpService.executeConnectorRequest(any(HttpCommonRequest.class)))
        .thenThrow(new RuntimeException("test exception"));

    // When
    task.run();

    // Then
    verify(mockProcessInstanceContext, never()).correlate(any());
  }

  @Test
  public void shouldNotExecuteHttpRequestIfNoBindingFound()
      throws IOException, InstantiationException, IllegalAccessException {
    // Given
    HttpRequestTask task = new HttpRequestTask(mockHttpService, mockProcessInstanceContext);
    when(mockProcessInstanceContext.bind(HttpCommonRequest.class)).thenReturn(null);

    // When
    task.run();

    // Then
    verify(mockHttpService, never()).executeConnectorRequest(any());
    verify(mockProcessInstanceContext, never()).correlate(any());
  }
}
