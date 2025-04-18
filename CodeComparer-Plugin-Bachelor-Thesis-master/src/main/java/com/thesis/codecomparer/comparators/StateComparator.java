package com.thesis.codecomparer.comparators;

import com.thesis.codecomparer.dataModels.BreakpointState;
import com.thesis.codecomparer.dataModels.ExceptionDetails;
import com.thesis.codecomparer.dataModels.MethodState;
import com.thesis.codecomparer.dataModels.ParameterInfo;
import java.util.ArrayList;
import java.util.List;

public class StateComparator {

  /**
   * Compares two BreakpointState objects and identifies differences.
   *
   * @param state1 The first BreakpointState object.
   * @param state2 The second BreakpointState object.
   * @return A list of strings describing the differences between the two BreakpointState objects.
   */
  public static List<String> compareBreakpointStates(
      BreakpointState state1, String file1Name, BreakpointState state2, String file2Name) {
    List<String> differences = new ArrayList<>();

    // Compare class fields
    if (!state1.getFileName().equals(state2.getFileName())) {
      differences.add("  - Class Name: " + state1.getFileName() + " != " + state2.getFileName());
    }

    // Compare breakpointInLine fields
    if (state1.getLineNumber() != state2.getLineNumber()) {
      differences.add("  - Line: " + state1.getLineNumber() + " != " + state2.getLineNumber());
    }

    // Compare the currentMethodState fields
    differences.addAll(
        compareMethodStates(
            state1.getCurrentMethodState(), state2.getCurrentMethodState(), "  - Current Method"));

    // Compare the breakpointMethodCallState fields
    differences.addAll(
        compareMethodStates(
            state1.getInvokedMethodState(), state2.getInvokedMethodState(), "  - Invoked Method"));

    // Compare breakpointReturnValue fields
    String returnValue1 = state1.getInvokedMethodReturnValue();
    String returnValue2 = state2.getInvokedMethodReturnValue();
    if (returnValue1 != null && returnValue2 != null) {
      if (!returnValue1.equals(returnValue2)) {
        differences.add("  - Return Value: " + returnValue1 + " != " + returnValue2);
      }
    } else if (returnValue1 != null || returnValue2 != null) {
      differences.add(
          "  - Return Value: "
              + (returnValue1 == null ? "null" : returnValue1)
              + " != "
              + (returnValue2 == null ? "null" : returnValue2));
    }

    // Compare exception info
    ExceptionDetails exception1 = state1.getExceptionDetails();
    ExceptionDetails exception2 = state2.getExceptionDetails();
    if (exception1 != null || exception2 != null) {
      if (exception1 == null) {
        differences.add(
            "  - Exception Info: Just File "
                + file1Name
                + " has an exception: \n"
                + formatExceptionInfo(state2.getExceptionDetails()));
      } else if (exception2 == null) {
        differences.add(
            "  - Exception Info: Just File "
                + file2Name
                + " has an exception: \n"
                + formatExceptionInfo(state1.getExceptionDetails()));
      } else {
        if (!exception1.getExceptionType().equals(exception2.getExceptionType())) {
          differences.add(
              "  - Exception Type: "
                  + exception1.getExceptionType()
                  + " != "
                  + exception2.getExceptionType());
        }

        if (!exception1.getExceptionMessage().equals(exception2.getExceptionMessage())) {
          differences.add(
              "  - Exception Message: "
                  + exception1.getExceptionMessage()
                  + " != "
                  + exception2.getExceptionMessage());
        }

        if (!exception1.getStackTrace().equals(exception2.getStackTrace())) {
          differences.add(
              "  - Exception Stack Trace: "
                  + exception1.getStackTrace()
                  + " != "
                  + exception2.getStackTrace());
        }
      }
    }
    return differences; // Return the list of differences
  }

  private static String formatExceptionInfo(ExceptionDetails exceptionInfo) {
    return "Type: "
        + exceptionInfo.getExceptionType()
        + "\n Message: "
        + exceptionInfo.getExceptionMessage()
        + "\n Stack Trace: "
        + exceptionInfo.getStackTrace();
  }

  /**
   * Compares two MethodState objects and identifies differences.
   *
   * @param method1 The first MethodState object.
   * @param method2 The second MethodState object.
   * @param context Context string for labeling differences.
   * @return A list of strings describing the differences between the two MethodState objects.
   */
  private static List<String> compareMethodStates(
      MethodState method1, MethodState method2, String context) {
    List<String> differences = new ArrayList<>();

    // Compare methodName fields
    if (!method1.getMethodName().equals(method2.getMethodName())) {
      differences.add(
          context + " -> Name: " + method1.getMethodName() + " != " + method2.getMethodName());
    }

    // Compare returnType fields
    if (!method1.getReturnType().equals(method2.getReturnType())) {
      differences.add(
          context
              + " -> Return Type: "
              + method1.getReturnType()
              + " != "
              + method2.getReturnType());
    }

    // Compare the size of the arguments lists
    if (method1.getParameters().size() != method2.getParameters().size()) {
      differences.add(
          context
              + " -> Arguments: size mismatch ("
              + method1.getParameters().size()
              + " != "
              + method2.getParameters().size()
              + ")");
    }
    // Compare individual arguments if sizes match
    else {
      for (int i = 0; i < method1.getParameters().size(); i++) {
        ParameterInfo arg1 = method1.getParameters().get(i);
        ParameterInfo arg2 = method2.getParameters().get(i);

        // Compare argument names
        if (!arg1.getName().equals(arg2.getName())) {
          differences.add(
              context
                  + " -> Argument["
                  + i
                  + "] Name: "
                  + arg1.getName()
                  + " != "
                  + arg2.getName());
        }

        // Compare argument JSON representations
        if (!arg1.getSerializedValue().equals(arg2.getSerializedValue())) {
          differences.add(
              context
                  + " -> Argument["
                  + i
                  + "] JSON: "
                  + arg1.getSerializedValue()
                  + " != "
                  + arg2.getSerializedValue());
        }
      }
    }

    return differences; // Return the list of differences
  }
}
