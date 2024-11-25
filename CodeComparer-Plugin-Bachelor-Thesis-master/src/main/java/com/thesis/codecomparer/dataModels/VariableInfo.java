package com.thesis.codecomparer.dataModels;

/**
 * Represents a variable and its value during debugging. This class encapsulates the variable's name
 * and its JSON representation, which provides a serialized view of the variable's value.
 */
public class VariableInfo {
  private String name; // Variable name
  private String jsonRepresentation; // JSON representation of the variable value

  public VariableInfo(String name, String jsonRepresentation) {
    this.name = name;
    this.jsonRepresentation = jsonRepresentation;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getJsonRepresentation() {
    return jsonRepresentation;
  }

  public void setJsonRepresentation(String jsonRepresentation) {
    this.jsonRepresentation = jsonRepresentation;
  }
}
