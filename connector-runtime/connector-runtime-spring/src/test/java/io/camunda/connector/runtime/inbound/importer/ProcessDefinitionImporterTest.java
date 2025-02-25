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
package io.camunda.connector.runtime.inbound.importer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.camunda.connector.runtime.inbound.lifecycle.InboundConnectorManager;
import io.camunda.operate.dto.ProcessDefinition;
import io.camunda.zeebe.spring.client.metrics.DefaultNoopMetricsRecorder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProcessDefinitionImporterTest {

  private ProcessDefinitionImporter importer;
  private InboundConnectorManager manager;

  @BeforeEach
  public void init() {
    manager = mock(InboundConnectorManager.class);
    var search = mock(ProcessDefinitionSearch.class);
    importer = new ProcessDefinitionImporter(manager, search, new DefaultNoopMetricsRecorder());
  }

  @Test
  void newProcessDefinitionDeployed_shouldRegister() {
    // given
    List<ProcessDefinition> first = List.of(getProcessDefinition("process1", 1, 1));
    List<ProcessDefinition> second =
        List.of(getProcessDefinition("process1", 1, 1), getProcessDefinition("process2", 1, 2));

    // when
    importer.handleImportedDefinitions(first);
    importer.handleImportedDefinitions(second);

    // then
    verify(manager, times(1)).handleNewProcessDefinitions(eq(new HashSet<>(first)));
    verify(manager, times(0)).handleDeletedProcessDefinitions(any());
    verify(manager, times(1)).handleNewProcessDefinitions(Set.of(second.get(1)));
  }

  @Test
  void newVersionDeployed_shouldRegister() {
    // given
    List<ProcessDefinition> first = List.of(getProcessDefinition("process1", 1, 1));
    List<ProcessDefinition> second =
        List.of(getProcessDefinition("process1", 1, 1), getProcessDefinition("process1", 2, 2));

    // when
    importer.handleImportedDefinitions(first);
    importer.handleImportedDefinitions(second);

    // then
    verify(manager, times(1)).handleNewProcessDefinitions(new HashSet<>(first));
    verify(manager, times(1)).handleDeletedProcessDefinitions(Set.of(first.get(0).getVersion()));
    verify(manager, times(1)).handleNewProcessDefinitions(Set.of(second.get(1)));

    // verify old version was deregistered and no action is taken on the next polling iteration
    importer.handleImportedDefinitions(second);
    verifyNoMoreInteractions(manager);
  }

  @Test
  void versionNotChanged_shouldNotRegister() {
    // given
    List<ProcessDefinition> definitions =
        List.of(getProcessDefinition("process1", 1, 1), getProcessDefinition("process2", 1, 2));

    // when
    importer.handleImportedDefinitions(definitions);
    importer.handleImportedDefinitions(definitions);

    // then
    verify(manager, times(1)).handleNewProcessDefinitions(new HashSet<>(definitions));
    verifyNoMoreInteractions(manager);
  }

  @Test
  void processDefinitionDeleted_shouldDeregister() {
    // given
    List<ProcessDefinition> first =
        List.of(getProcessDefinition("process1", 1, 1), getProcessDefinition("process2", 1, 2));

    List<ProcessDefinition> second = List.of(getProcessDefinition("process1", 1, 1));

    // when
    importer.handleImportedDefinitions(first);
    importer.handleImportedDefinitions(second);

    // then
    verify(manager, times(1)).handleNewProcessDefinitions(new HashSet<>(first));
    verify(manager, times(1)).handleDeletedProcessDefinitions(Set.of(first.get(1).getKey()));
  }

  @Test
  void newerVersionDeleted_shouldRegisterOldOne() {
    // given
    List<ProcessDefinition> first =
        List.of(getProcessDefinition("process1", 1, 1), getProcessDefinition("process1", 2, 2));

    List<ProcessDefinition> second = List.of(getProcessDefinition("process1", 1, 1));

    // when
    importer.handleImportedDefinitions(first);
    importer.handleImportedDefinitions(second);

    // then
    verify(manager, times(1)).handleNewProcessDefinitions(Set.of(first.get(1)));
    verify(manager, times(1)).handleDeletedProcessDefinitions(Set.of(first.get(1).getVersion()));
    verify(manager, times(1)).handleNewProcessDefinitions(new HashSet<>(second));
  }

  private ProcessDefinition getProcessDefinition(String bpmnProcessId, long version, long key) {
    var pd = new ProcessDefinition();
    pd.setBpmnProcessId(bpmnProcessId);
    pd.setKey(key);
    pd.setVersion(version);
    return pd;
  }
}
