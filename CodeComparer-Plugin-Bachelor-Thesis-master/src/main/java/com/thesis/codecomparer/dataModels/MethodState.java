package com.thesis.codecomparer.dataModels;

import java.util.List;

public class MethodState {
  private String methodName; // Name of the method
  private String returnType; // Return type of the method
  private List<VariableInfo> arguments; // Arguments passed to the method

  public String getMethodName() {
    return methodName;
  }

  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  public String getReturnType() {
    return returnType;
  }

  public void setReturnType(String returnType) {
    this.returnType = returnType;
  }

  public List<VariableInfo> getArguments() {
    return arguments;
  }

  public void setArguments(List<VariableInfo> arguments) {
    this.arguments = arguments;
  }
}
