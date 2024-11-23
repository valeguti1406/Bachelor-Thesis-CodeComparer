package com.thesis.codecomparer.Comparators;

import com.thesis.codecomparer.dataModels.BreakpointState;
import com.thesis.codecomparer.dataModels.MethodState;
import com.thesis.codecomparer.dataModels.VariableInfo;

import java.util.ArrayList;
import java.util.List;

public class StateComparator {

    public static List<String> compareStates(List<BreakpointState> file1States, List<BreakpointState> file2States) {
        List<String> differences = new ArrayList<>();

        int maxSize = Math.max(file1States.size(), file2States.size());

        for (int i = 0; i < maxSize; i++) {
            if (i >= file1States.size()) {
                differences.add("Extra block in file 2: " + file2States.get(i));
            } else if (i >= file2States.size()) {
                differences.add("Extra block in file 1: " + file1States.get(i));
            } else {
                differences.addAll(compareBreakpointStates(file1States.get(i), file2States.get(i), "Breakpoint " + (i + 1)));
            }
        }

        return differences;
    }

    static List<String> compareBreakpointStates(BreakpointState state1, BreakpointState state2, String path) {
        List<String> differences = new ArrayList<>();

        // Compare fields
        if (!state1.getFileName().equals(state2.getFileName())) {
            differences.add(path + " -> fileName: " + state1.getFileName() + " != " + state2.getFileName());
        }

        if (state1.getBreakpointInLine() != state2.getBreakpointInLine()) {
            differences.add(path + " -> breakpointInLine: " + state1.getBreakpointInLine() + " != " + state2.getBreakpointInLine());
        }

        differences.addAll(compareMethodStates(state1.getCurrentMethodState(), state2.getCurrentMethodState(), path + " -> currentMethodState"));
        differences.addAll(compareMethodStates(state1.getBreakpointMethodCallState(), state2.getBreakpointMethodCallState(), path + " -> breakpointMethodCallState"));

        if (!state1.getBreakpointReturnValue().equals(state2.getBreakpointReturnValue())) {
            differences.add(path + " -> breakpointReturnValue: " + state1.getBreakpointReturnValue() + " != " + state2.getBreakpointReturnValue());
        }

        return differences;
    }

    private static List<String> compareMethodStates(MethodState method1, MethodState method2, String path) {
        List<String> differences = new ArrayList<>();

        if (!method1.getMethodName().equals(method2.getMethodName())) {
            differences.add(path + " -> methodName: " + method1.getMethodName() + " != " + method2.getMethodName());
        }

        if (!method1.getReturnType().equals(method2.getReturnType())) {
            differences.add(path + " -> returnType: " + method1.getReturnType() + " != " + method2.getReturnType());
        }

        if (method1.getArguments().size() != method2.getArguments().size()) {
            differences.add(path + " -> arguments size mismatch: " + method1.getArguments().size() + " != " + method2.getArguments().size());
        } else {
            for (int i = 0; i < method1.getArguments().size(); i++) {
                VariableInfo arg1 = method1.getArguments().get(i);
                VariableInfo arg2 = method2.getArguments().get(i);

                if (!arg1.getName().equals(arg2.getName())) {
                    differences.add(path + " -> arguments[" + i + "] -> name: " + arg1.getName() + " != " + arg2.getName());
                }
                if (!arg1.getJsonRepresentation().equals(arg2.getJsonRepresentation())) {
                    differences.add(path + " -> arguments[" + i + "] -> jsonRepresentation: " + arg1.getJsonRepresentation() + " != " + arg2.getJsonRepresentation());
                }
            }
        }

        return differences;
    }

}
