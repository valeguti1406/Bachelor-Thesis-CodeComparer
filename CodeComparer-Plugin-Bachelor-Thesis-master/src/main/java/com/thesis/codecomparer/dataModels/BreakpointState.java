package com.thesis.codecomparer.dataModels;

/**
 * Represents the state of a breakpoint during debugging.
 * This class encapsulates information about the file, line number,
 * the current method containing the breakpoint, the method called by the breakpoint,
 * and the return value at the breakpoint.
 */
public class BreakpointState {

  private String fileName; // File where the breakpoint is set
  private int breakpointInLine; // Line number of the breakpoint
  private MethodState currentMethodState; // Details about the method containing the breakpoint
  private MethodState
      breakpointMethodCallState; // Details about the method called at the breakpoint
  private String breakpointReturnValue; // Return value of the code executed at this breakpoint

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public int getBreakpointInLine() {
    return breakpointInLine;
  }

  public void setBreakpointInLine(int breakpointInLine) {
    this.breakpointInLine = breakpointInLine;
  }

  public MethodState getCurrentMethodState() {
    return currentMethodState;
  }

  public void setCurrentMethodState(MethodState currentMethodState) {
    this.currentMethodState = currentMethodState;
  }

  public String getBreakpointReturnValue() {
    return breakpointReturnValue;
  }

  public void setBreakpointReturnValue(String breakpointReturnValue) {
    this.breakpointReturnValue = breakpointReturnValue;
  }

  public MethodState getBreakpointMethodCallState() {
    return breakpointMethodCallState;
  }

  public void setBreakpointMethodCallState(MethodState breakpointMethodCallState) {
    this.breakpointMethodCallState = breakpointMethodCallState;
  }
}
