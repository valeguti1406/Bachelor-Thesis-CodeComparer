package com.thesis.codecomparer.dataModels;

/**
 * Represents the state of a breakpoint during debugging. This class encapsulates information about
 * the file, line number, the current method containing the breakpoint, the method called by the
 * breakpoint, and the return value at the breakpoint.
 */
public class BreakpointState {

  private String fileName; // File where the breakpoint is set
  private int lineNumber; // Line number of the breakpoint
  private MethodState currentMethodState; // Details about the method containing the breakpoint
  private MethodState invokedMethodState; // Details about the method called at the breakpoint
  private String invokedMethodReturnValue; // Return value of the invoked method at the breakpoint

  private ExceptionDetails
      exceptionDetails; // Exception details if an exception occurred at the breakpoint

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

  public MethodState getCurrentMethodState() {
    return currentMethodState;
  }

  public void setCurrentMethodState(MethodState currentMethodState) {
    this.currentMethodState = currentMethodState;
  }

  public String getInvokedMethodReturnValue() {
    return invokedMethodReturnValue;
  }

  public void setInvokedMethodReturnValue(String invokedMethodReturnValue) {
    this.invokedMethodReturnValue = invokedMethodReturnValue;
  }

  public MethodState getInvokedMethodState() {
    return invokedMethodState;
  }

  public void setInvokedMethodState(MethodState invokedMethodState) {
    this.invokedMethodState = invokedMethodState;
  }

  public ExceptionDetails getExceptionDetails() {
    return exceptionDetails;
  }

  public void setExceptionDetails(ExceptionDetails exceptionDetails) {
    this.exceptionDetails = exceptionDetails;
  }
}
