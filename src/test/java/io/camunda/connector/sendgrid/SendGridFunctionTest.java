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

package io.camunda.connector.sendgrid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import io.camunda.connector.api.ConnectorContext;
import io.camunda.connector.test.ConnectorContextBuilder;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

public class SendGridFunctionTest extends BaseTest {

  private static final String TEMPLATE_ID_JSON_NAME = "template_id";
  private static final String CONTENT_JSON_NAME = "content";
  private static final String CONTENT_TYPE_JSON_NAME = "type";
  private static final String CONTENT_VALUE_JSON_NAME = "value";
  private static final String SUBJECT_JSON_NAME = "subject";
  private static final String FROM_JSON_NAME = "from";
  private static final String TO_JSON_NAME = "to";
  private static final String PERSONALIZATION_JSON_NAME = "personalizations";
  private static final String NAME_JSON_NAME = "name";
  private static final String EMAIL_JSON_NAME = "email";

  private ConnectorContext context;
  private SendGridFunction function;
  private Response sendGridResponse;
  private SendGrid sendGridMock;
  private ArgumentCaptor<Request> requestArgumentCaptor;
  private ConnectorContextBuilder contextBuilder;

  @BeforeEach
  public void init() throws IOException {
    contextBuilder = getContextBuilderWithSecrets();

    SendGridErrors sendGridErrors = new SendGridErrors();
    SendGridErrors.SendGridError sendGridError = new SendGridErrors.SendGridError();
    sendGridError.setMessage("error msg");
    sendGridErrors.setErrors(List.of(sendGridError));
    gson.toJson(sendGridErrors);

    sendGridResponse = new Response();
    sendGridResponse.setBody(gson.toJson(sendGridErrors));
    sendGridResponse.setStatusCode(202);

    SendGridClientSupplier sendGridSupplierMock = mock(SendGridClientSupplier.class);
    sendGridMock = mock(SendGrid.class);

    when(sendGridSupplierMock.sendGrid(any())).thenReturn(sendGridMock);
    when(sendGridMock.api(any())).thenReturn(sendGridResponse);
    function = new SendGridFunction(gson, sendGridSupplierMock);

    requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);
  }

  @ParameterizedTest(name = " # {index} , test statusCode = {0}")
  @ValueSource(ints = {100, 200, 201, 203, 303, 400, 404})
  public void execute_shouldThrowExceptionIfResponseStatusCodeIsNot202(int statusCode) {
    // Given
    SendGridRequest request = mock(SendGridRequest.class);
    context = contextBuilder.variables(request).build();
    sendGridResponse.setStatusCode(statusCode);
    // When and then
    IllegalArgumentException exceptionThrown =
        Assertions.assertThrows(IllegalArgumentException.class, () -> function.execute(context));

    assertThat(exceptionThrown)
        .hasMessageContaining("SendGrid returned the following errors:", statusCode);
  }

  @ParameterizedTest(name = " # {index} , test statusCode = {0}")
  @ValueSource(ints = {202})
  public void execute_shouldReturnNullIfResponseStatusCodeIs202(int statusCode) throws Exception {
    // Given
    SendGridRequest request = mock(SendGridRequest.class);
    context = contextBuilder.variables(request).build();
    sendGridResponse.setStatusCode(statusCode);
    // When
    Object execute = function.execute(context);
    // Then no exception and result is null
    assertThat(execute).isNull();
  }

  @ParameterizedTest(
      name = "Should create request with mail and expected data. Test case # {index}")
  @MethodSource("successSendMailWithContentRequestCases")
  public void execute_shouldCreateRequestWithMailAndExpectedData(String input) throws Exception {
    // Given
    context = contextBuilder.variables(gson.fromJson(input, SendGridRequest.class)).build();
    ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);
    // When
    function.execute(context);
    verify(sendGridMock).api(requestArgumentCaptor.capture());
    // Then we have POST request with mail participants,
    Request requestValue = requestArgumentCaptor.getValue();
    assertThat(requestValue.getMethod()).isEqualTo(Method.POST);

    JsonObject requestJsonObject = gson.fromJson(requestValue.getBody(), JsonObject.class);
    JsonObject from = requestJsonObject.get(FROM_JSON_NAME).getAsJsonObject();

    JsonObject to =
        requestJsonObject
            .get(PERSONALIZATION_JSON_NAME)
            .getAsJsonArray()
            .get(0)
            .getAsJsonObject()
            .get(TO_JSON_NAME)
            .getAsJsonArray()
            .get(0)
            .getAsJsonObject();

    assertThat(from.get(NAME_JSON_NAME).getAsString()).isEqualTo(ActualValue.SENDER_NAME);
    assertThat(from.get(EMAIL_JSON_NAME).getAsString()).isEqualTo(ActualValue.SENDER_EMAIL);
    assertThat(to.get(NAME_JSON_NAME).getAsString()).isEqualTo(ActualValue.RECEIVER_NAME);
    assertThat(to.get(EMAIL_JSON_NAME).getAsString()).isEqualTo(ActualValue.RECEIVER_EMAIL);
  }

  @ParameterizedTest(name = "Should send mail with template. Test case # {index}")
  @MethodSource("successSendMailByTemplateRequestCases")
  public void execute_shouldSendMailByTemplateIfTemplateExist(String input) throws Exception {
    // Given
    context = contextBuilder.variables(gson.fromJson(input, SendGridRequest.class)).build();
    ArgumentCaptor<Request> requestArgumentCaptor = ArgumentCaptor.forClass(Request.class);
    // When
    function.execute(context);
    verify(sendGridMock).api(requestArgumentCaptor.capture());
    // Then we have 'template_id' in sendGridRequest with expected ID and 'content' is not exist
    Request requestValue = requestArgumentCaptor.getValue();
    JsonObject requestJsonObject = gson.fromJson(requestValue.getBody(), JsonObject.class);
    assertThat(requestJsonObject.get(TEMPLATE_ID_JSON_NAME).getAsString())
        .isEqualTo(ActualValue.Template.ID);
    assertThat(requestJsonObject.has(CONTENT_JSON_NAME)).isFalse();
  }

  @ParameterizedTest(name = "Should send mail with content. Test case # {index}")
  @MethodSource("successSendMailWithContentRequestCases")
  public void execute_shouldSendMailIfContentExist(String input) throws Exception {
    // Given
    context = contextBuilder.variables(gson.fromJson(input, SendGridRequest.class)).build();
    // When
    function.execute(context);
    verify(sendGridMock).api(requestArgumentCaptor.capture());
    // Then we have 'content' in sendGridRequest with expected data and template ID is not exist
    JsonObject requestJsonObject =
        gson.fromJson(requestArgumentCaptor.getValue().getBody(), JsonObject.class);

    assertThat(requestJsonObject.get(SUBJECT_JSON_NAME).getAsString())
        .isEqualTo(ActualValue.Content.SUBJECT);
    JsonObject content =
        requestJsonObject.get(CONTENT_JSON_NAME).getAsJsonArray().get(0).getAsJsonObject();
    assertThat(content.get(CONTENT_TYPE_JSON_NAME).getAsString())
        .isEqualTo(ActualValue.Content.TYPE);
    assertThat(content.get(CONTENT_VALUE_JSON_NAME).getAsString())
        .isEqualTo(ActualValue.Content.VALUE);

    assertThat(requestJsonObject.has(TEMPLATE_ID_JSON_NAME)).isFalse();
  }
}
