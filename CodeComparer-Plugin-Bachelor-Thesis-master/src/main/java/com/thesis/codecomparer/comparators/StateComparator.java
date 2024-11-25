package com.thesis.codecomparer.comparators;

import com.thesis.codecomparer.dataModels.BreakpointState;
import com.thesis.codecomparer.dataModels.MethodState;
import com.thesis.codecomparer.dataModels.VariableInfo;
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
      BreakpointState state1, BreakpointState state2) {
    List<String> differences = new ArrayList<>();

    // Compare fileName fields
    if (!state1.getFileName().equals(state2.getFileName())) {
      differences.add("  - File Name: " + state1.getFileName() + " != " + state2.getFileName());
    }

    // Compare breakpointInLine fields
    if (state1.getBreakpointInLine() != state2.getBreakpointInLine()) {
      differences.add(
          "  - Line: " + state1.getBreakpointInLine() + " != " + state2.getBreakpointInLine());
    }

    // Compare the currentMethodState fields
    differences.addAll(
        compareMethodStates(
            state1.getCurrentMethodState(), state2.getCurrentMethodState(), "  - Current Method"));

    // Compare the breakpointMethodCallState fields
    differences.addAll(
        compareMethodStates(
            state1.getBreakpointMethodCallState(),
            state2.getBreakpointMethodCallState(),
            "  - Invoked Method"));

    // Compare breakpointReturnValue fields
    if (!state1.getBreakpointReturnValue().equals(state2.getBreakpointReturnValue())) {
      differences.add(
          "  - Return Value: "
              + state1.getBreakpointReturnValue()
              + " != "
              + state2.getBreakpointReturnValue());
    }

    return differences; // Return the list of differences
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
    if (method1.getArguments().size() != method2.getArguments().size()) {
      differences.add(
          context
              + " -> Arguments: size mismatch ("
              + method1.getArguments().size()
              + " != "
              + method2.getArguments().size()
              + ")");
    }
    // Compare individual arguments if sizes match
    else {
      for (int i = 0; i < method1.getArguments().size(); i++) {
        VariableInfo arg1 = method1.getArguments().get(i);
        VariableInfo arg2 = method2.getArguments().get(i);

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
        if (!arg1.getJsonRepresentation().equals(arg2.getJsonRepresentation())) {
          differences.add(
              context
                  + " -> Argument["
                  + i
                  + "] JSON: "
                  + arg1.getJsonRepresentation()
                  + " != "
                  + arg2.getJsonRepresentation());
        }
      }
    }

    return differences; // Return the list of differences
  }
}
