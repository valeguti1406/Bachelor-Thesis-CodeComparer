package com.thesis.codecomparer.dataModels;

public class BreakpointState {

  private String fileName; // File where the breakpoint is set
  private int lineNumber; // Line number of the breakpoint
  private String returnValue; // Return value of the code executed at this breakpoint
  private MethodState methodState; // Details about the method being executed at the breakpoint

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public void setLineNumber(int lineNumber) {
    this.lineNumber = lineNumber;
  }

  public String getReturnValue() {
    return returnValue;
  }

  public void setReturnValue(String returnValue) {
    this.returnValue = returnValue;
  }

  public MethodState getMethodState() {
    return methodState;
  }

  public void setMethodState(MethodState methodState) {
    this.methodState = methodState;
  }
}
