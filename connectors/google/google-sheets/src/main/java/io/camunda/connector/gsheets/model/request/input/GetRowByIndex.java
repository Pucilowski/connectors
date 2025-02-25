/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.gsheets.model.request.input;

import jakarta.validation.constraints.NotNull;
import java.util.Objects;

public final class GetRowByIndex extends SpreadsheetInput {

  private String worksheetName;
  @NotNull private Integer rowIndex;

  public GetRowByIndex() {}

  public GetRowByIndex(String spreadsheetId, String worksheetName, Integer rowIndex) {
    super(spreadsheetId);
    this.worksheetName = worksheetName;
    this.rowIndex = rowIndex;
  }

  public String getWorksheetName() {
    return worksheetName;
  }

  public void setWorksheetName(String worksheetName) {
    this.worksheetName = worksheetName;
  }

  public Integer getRowIndex() {
    return rowIndex;
  }

  public void setRowIndex(Integer rowIndex) {
    this.rowIndex = rowIndex;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GetRowByIndex that = (GetRowByIndex) o;
    return Objects.equals(worksheetName, that.worksheetName)
        && Objects.equals(rowIndex, that.rowIndex);
  }

  @Override
  public int hashCode() {
    return Objects.hash(worksheetName, rowIndex);
  }

  @Override
  public String toString() {
    return "GetRowByIndex{"
        + "worksheetName='"
        + worksheetName
        + '\''
        + ", rowIndex="
        + rowIndex
        + "} "
        + super.toString();
  }
}
