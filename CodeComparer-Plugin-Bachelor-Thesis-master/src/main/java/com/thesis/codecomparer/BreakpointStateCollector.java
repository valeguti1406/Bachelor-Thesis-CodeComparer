package com.thesis.codecomparer;

import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.JavaStackFrame;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.jdi.LocalVariableProxyImpl;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.sun.jdi.*;
import com.thesis.codecomparer.VariableExtractor.ValueJsonSerializer;
import com.thesis.codecomparer.dataModels.MethodState;
import com.thesis.codecomparer.dataModels.VariableInfo;
import java.util.*;
import org.jetbrains.annotations.NotNull;

public class BreakpointStateCollector {

  private static final Logger LOGGER = Logger.getInstance(BreakpointStateCollector.class);

  // Represents the current stack frame to analyze
  private final StackFrameProxyImpl stackFrame;

  public BreakpointStateCollector(@NotNull StackFrameProxyImpl stackFrame) {
    this.stackFrame = stackFrame;
  }

  /*
   Extract from the Method tha the debugger is currently in, the inputs and outputs
  */
  public MethodState getMethodState(@NotNull JavaStackFrame currentStackFrame) {
    MethodState methodState = new MethodState();

    try {
      Method currentMethod = currentStackFrame.getStackFrameProxy().location().method();

      // Set method name and return type
      methodState.setMethodName(currentMethod.name());
      methodState.setReturnType(currentMethod.returnType().name());

      // Collect arguments
      List<VariableInfo> argumentInfos = extractArgumentsInfo(currentStackFrame, currentMethod);
      methodState.setArguments(argumentInfos);

      return methodState;
    } catch (EvaluateException e) {
      LOGGER.warn("Current method could not be found!");
      throw new RuntimeException(e);
    } catch (ClassNotLoadedException e) {
      LOGGER.warn("Getting the return type of the current method was not possible");
      throw new RuntimeException(e);
    }
  }

  public String getReturnValue(JavaStackFrame currentStackFrame) {
    LOGGER.warn("In getReturnValue");
    try {
      Pair<Method, Value> methodValuePair =
          getDebugProcess(currentStackFrame).getLastExecutedMethod();
      if (methodValuePair != null) {
        Value returnValue = methodValuePair.getSecond();

        // Convert return value to JSON
        return extractVariableJson(currentStackFrame, returnValue);
      }
    } catch (Exception e) {
      LOGGER.warn("Error collecting return value", e);
      throw new RuntimeException(e);
    }
    return "There is no return value for the last executed method";
  }

  private static DebugProcessImpl getDebugProcess(JavaStackFrame javaStackFrame) {
    try {
      // Get the class of the JavaStackFrame instance
      Class<?> clazz = javaStackFrame.getClass();

      // Find the private field "myDebugProcess"
      java.lang.reflect.Field debugProcessField = clazz.getDeclaredField("myDebugProcess");

      // Make it accessible
      debugProcessField.setAccessible(true);

      // Get the value of the field from the given instance
      return (DebugProcessImpl) debugProcessField.get(javaStackFrame);
    } catch (Exception e) {
      LOGGER.warn("Failed to access myDebugProcess");
      throw new RuntimeException("Failed to access myDebugProcess", e);
    }
  }

  /**
   * Get type, name and value of the arguments of a method and add this information to the string
   * builder
   */
  private List<VariableInfo> extractArgumentsInfo(
      JavaStackFrame currentStackFrame, Method currentMethod) {
    List<VariableInfo> argumentInfos = new ArrayList<>();

    try {
      List<LocalVariable> methodArguments = currentMethod.arguments();

      for (LocalVariable argument : methodArguments) {

        String argumentName = argument.name();
        LocalVariableProxyImpl argumentLocalVariable =
            currentStackFrame.getStackFrameProxy().visibleVariableByName(argumentName);
        Value argumentValue = stackFrame.getValue(argumentLocalVariable);

        // Convert variable to JSON and add to list
        String jsonRepresentation = extractVariableJson(currentStackFrame, argumentValue);
        argumentInfos.add(new VariableInfo(argumentName, jsonRepresentation));
      }
    } catch (AbsentInformationException e) {
      LOGGER.warn("Getting Method Arguments was not possible");
      throw new RuntimeException(e);
    } catch (EvaluateException e) {
      LOGGER.warn("Getting variable of Stack Frame was not possible");
      throw new RuntimeException(e);
    }
    return argumentInfos;
  }

  private String extractVariableJson(JavaStackFrame currentStackFrame, Value value) {
    // Get the thread reference from the current stack frame
    ThreadReference threadReference =
        currentStackFrame.getStackFrameProxy().threadProxy().getThreadReference();

    // Convert the variable's value to JSON
    return ValueJsonSerializer.toJson(value, threadReference, new HashSet<>());
  }
}
