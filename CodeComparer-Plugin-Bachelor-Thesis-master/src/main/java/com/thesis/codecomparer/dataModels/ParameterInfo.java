package com.thesis.codecomparer.dataModels;

/**
 * Represents a parameter and its value during debugging.
 * This class encapsulates the parameter's name and its serialized value,
 * which provides a structured representation of the variable's state.
 */
public class ParameterInfo {
  private String name; // Name of the parameter as defined in the method signature
  private String serializedValue; /// Serialized representation of the parameter's value

  public ParameterInfo(String name, String jsonRepresentation) {
    this.name = name;
    this.serializedValue = jsonRepresentation;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSerializedValue() {
    return serializedValue;
  }

  public void setSerializedValue(String serializedValue) {
    this.serializedValue = serializedValue;
  }
}
