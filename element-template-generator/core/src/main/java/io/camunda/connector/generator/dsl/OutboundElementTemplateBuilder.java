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
package io.camunda.connector.generator.dsl;

import io.camunda.connector.generator.dsl.Property.FeelMode;
import io.camunda.connector.generator.dsl.PropertyBinding.ZeebeTaskDefinitionType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class OutboundElementTemplateBuilder {

  private String id;
  private String name;
  private int version;
  private ElementTemplateIcon icon;
  private String documentationRef;
  private String description;
  private final List<PropertyGroup> groups = new ArrayList<>();
  private final List<Property> properties = new ArrayList<>();

  private OutboundElementTemplateBuilder() {}

  public static OutboundElementTemplateBuilder create() {
    return new OutboundElementTemplateBuilder();
  }

  public OutboundElementTemplateBuilder id(String id) {
    this.id = id;
    return this;
  }

  public OutboundElementTemplateBuilder type(String type, boolean configurable) {
    if (isTypeAssigned()) {
      throw new IllegalStateException("type is already assigned");
    }
    Property property;
    if (configurable) {
      groups.add(
          0,
          PropertyGroup.builder().id("taskDefinitionType").label("Task definition type").build());
      property =
          StringProperty.builder()
              .binding(ZeebeTaskDefinitionType.INSTANCE)
              .value(type)
              .id("taskDefinitionType")
              .group("taskDefinitionType")
              .feel(FeelMode.disabled)
              .build();
    } else {
      property =
          HiddenProperty.builder().binding(ZeebeTaskDefinitionType.INSTANCE).value(type).build();
    }
    properties.add(property);
    return this;
  }

  public OutboundElementTemplateBuilder type(String type) {
    return type(type, false);
  }

  public OutboundElementTemplateBuilder name(String name) {
    this.name = name;
    return this;
  }

  public OutboundElementTemplateBuilder version(int version) {
    this.version = version;
    return this;
  }

  public OutboundElementTemplateBuilder icon(ElementTemplateIcon icon) {
    this.icon = icon;
    return this;
  }

  public OutboundElementTemplateBuilder documentationRef(String documentationRef) {
    this.documentationRef = documentationRef;
    return this;
  }

  public OutboundElementTemplateBuilder description(String description) {
    this.description = description;
    return this;
  }

  public OutboundElementTemplateBuilder propertyGroups(PropertyGroup... groups) {
    this.groups.addAll(Arrays.asList(groups));
    this.properties.addAll(
        this.groups.stream().flatMap(group -> group.properties().stream()).toList());
    return this;
  }

  public OutboundElementTemplateBuilder propertyGroups(Collection<PropertyGroup> groups) {
    this.groups.addAll(groups);
    this.properties.addAll(
        this.groups.stream().flatMap(group -> group.properties().stream()).toList());
    return this;
  }

  public OutboundElementTemplateBuilder properties(Property... properties) {
    this.properties.addAll(Arrays.asList(properties));
    return this;
  }

  public OutboundElementTemplateBuilder properties(Collection<Property> properties) {
    this.properties.addAll(properties);
    return this;
  }

  public OutboundElementTemplate build() {
    if (!isTypeAssigned()) {
      throw new IllegalStateException("type is not assigned");
    }
    verifyUniquePropertyIds();
    return new OutboundElementTemplate(
        id, name, version, documentationRef, description, groups, properties, icon);
  }

  private boolean isTypeAssigned() {
    return this.properties.stream()
        .anyMatch(property -> property.binding.type().equals(ZeebeTaskDefinitionType.NAME));
  }

  private void verifyUniquePropertyIds() {}
}
