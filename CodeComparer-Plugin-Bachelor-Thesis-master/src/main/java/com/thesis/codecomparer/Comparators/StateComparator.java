package com.thesis.codecomparer.Comparators;

import com.thesis.codecomparer.dataModels.BreakpointState;
import com.thesis.codecomparer.dataModels.MethodState;
import com.thesis.codecomparer.dataModels.VariableInfo;

import java.util.ArrayList;
import java.util.List;

public class StateComparator {

    /**
     * Compares two lists of BreakpointState objects and identifies differences.
     * Each difference is represented as a descriptive string.
     *
     * @param file1States The list of BreakpointState objects from the first file.
     * @param file2States The list of BreakpointState objects from the second file.
     * @return A list of strings describing the differences between the two lists.
     */
    public static List<String> compareStates(List<BreakpointState> file1States, List<BreakpointState> file2States) {
        List<String> differences = new ArrayList<>();

        // Determine the maximum size between the two lists
        int maxSize = Math.max(file1States.size(), file2States.size());

        // Iterate over both lists up to the maximum size
        for (int i = 0; i < maxSize; i++) {
            // If file 1 is shorter, mark the extra blocks in file 2
            if (i >= file1States.size()) {
                differences.add("Extra block in file 2: " + file2States.get(i));
            }
            // If file 2 is shorter, mark the extra blocks in file 1
            else if (i >= file2States.size()) {
                differences.add("Extra block in file 1: " + file1States.get(i));
            }
            // If both lists have a block at the current index, compare them
            else {
                differences.addAll(compareBreakpointStates(file1States.get(i), file2States.get(i), "Breakpoint " + (i + 1)));
            }
        }
        return differences;
    }

    /**
     * Compares two BreakpointState objects and identifies differences between their fields.
     * The differences are represented as descriptive strings, prefixed by the specified name.
     *
     * @param state1 The first BreakpointState object.
     * @param state2 The second BreakpointState object.
     * @param name   A string representing the current comparison context (e.g., "Breakpoint 1").
     * @return A list of strings describing the differences between the two BreakpointState objects.
     */
    static List<String> compareBreakpointStates(BreakpointState state1, BreakpointState state2, String name) {
        List<String> differences = new ArrayList<>();

        // Compare fileName fields
        if (!state1.getFileName().equals(state2.getFileName())) {
            differences.add(name + " -> fileName: " + state1.getFileName() + " != " + state2.getFileName());
        }

        // Compare breakpointInLine fields
        if (state1.getBreakpointInLine() != state2.getBreakpointInLine()) {
            differences.add(name + " -> breakpointInLine: " + state1.getBreakpointInLine() + " != " + state2.getBreakpointInLine());
        }

        // Compare the currentMethodState fields
        differences.addAll(compareMethodStates(state1.getCurrentMethodState(), state2.getCurrentMethodState(), name + " -> currentMethodState"));

        // Compare the breakpointMethodCallState fields
        differences.addAll(compareMethodStates(state1.getBreakpointMethodCallState(), state2.getBreakpointMethodCallState(), name + " -> breakpointMethodCallState"));

        // Compare breakpointReturnValue fields
        if (!state1.getBreakpointReturnValue().equals(state2.getBreakpointReturnValue())) {
            differences.add(name + " -> breakpointReturnValue: " + state1.getBreakpointReturnValue() + " != " + state2.getBreakpointReturnValue());
        }

        return differences; // Return the list of differences
    }


    /**
     * Compares two MethodState objects and identifies differences between their fields.
     * The differences are represented as descriptive strings, prefixed by the specified path.
     *
     * @param method1 The first MethodState object.
     * @param method2 The second MethodState object.
     * @param path    A string representing the current comparison context (e.g., "Breakpoint 1 -> currentMethodState").
     * @return A list of strings describing the differences between the two MethodState objects.
     */
    private static List<String> compareMethodStates(MethodState method1, MethodState method2, String path) {
        List<String> differences = new ArrayList<>();

        // Compare methodName fields
        if (!method1.getMethodName().equals(method2.getMethodName())) {
            differences.add(path + " -> methodName: " + method1.getMethodName() + " != " + method2.getMethodName());
        }

        // Compare returnType fields
        if (!method1.getReturnType().equals(method2.getReturnType())) {
            differences.add(path + " -> returnType: " + method1.getReturnType() + " != " + method2.getReturnType());
        }

        // Compare the size of the arguments lists
        if (method1.getArguments().size() != method2.getArguments().size()) {
            differences.add(path + " -> arguments size mismatch: " + method1.getArguments().size() + " != " + method2.getArguments().size());
        }
        // Compare individual arguments if sizes match
        else {
            for (int i = 0; i < method1.getArguments().size(); i++) {
                VariableInfo arg1 = method1.getArguments().get(i);
                VariableInfo arg2 = method2.getArguments().get(i);

                // Compare argument names
                if (!arg1.getName().equals(arg2.getName())) {
                    differences.add(path + " -> arguments[" + i + "] -> name: " + arg1.getName() + " != " + arg2.getName());
                }

                // Compare argument JSON representations
                if (!arg1.getJsonRepresentation().equals(arg2.getJsonRepresentation())) {
                    differences.add(path + " -> arguments[" + i + "] -> jsonRepresentation: " + arg1.getJsonRepresentation() + " != " + arg2.getJsonRepresentation());
                }
            }
        }

        return differences; // Return the list of differences
    }


}
