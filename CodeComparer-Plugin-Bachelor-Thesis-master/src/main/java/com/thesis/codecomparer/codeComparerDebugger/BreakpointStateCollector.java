package com.thesis.codecomparer.codeComparerDebugger;

import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.JavaStackFrame;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.jdi.LocalVariableProxyImpl;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.sun.jdi.*;
import com.thesis.codecomparer.dataModels.MethodState;
import com.thesis.codecomparer.dataModels.VariableInfo;
import com.thesis.codecomparer.ui.CodeComparerUI;
import com.thesis.codecomparer.variableSerializer.ValueJsonSerializer;
import java.util.*;
import org.jetbrains.annotations.NotNull;

/**
 * Collects state information about the current method being executed in a debugger session. This
 * includes: - Method name, return type, and arguments (inputs). - Return value of the last executed
 * method. - Serialized representations of arguments and return values in JSON.
 *
 * <p>The class relies on the IntelliJ Debugger API to extract runtime information from the current
 * stack frame.
 */
public class BreakpointStateCollector {

  private static final Logger LOGGER = Logger.getInstance(BreakpointStateCollector.class);

  private final StackFrameProxyImpl stackFrame; // Current stack frame to analyze
  private final CodeComparerUI codeComparerUI; // UI manager for the CodeComparer tab

  /**
   * Constructs a BreakpointStateCollector for a specific stack frame.
   *
   * @param stackFrame The stack frame to analyze.
   */
  public BreakpointStateCollector(@NotNull StackFrameProxyImpl stackFrame) {
    this.stackFrame = stackFrame;
    this.codeComparerUI = CodeComparerUI.getInstance();
  }

  /**
   * Extracts information about the current method being executed, including: - Method name and
   * return type. - Argument names and serialized JSON representations of their values.
   *
   * @param currentStackFrame The current stack frame in the debugger session.
   * @return A MethodState object containing method details and argument information.
   */
  public MethodState getMethodState(@NotNull JavaStackFrame currentStackFrame) {
    MethodState methodState = new MethodState();

    try {
      // Retrieve the method being executed in the current stack frame
      Method currentMethod = currentStackFrame.getStackFrameProxy().location().method();

      // Set method name and return type
      methodState.setMethodName(currentMethod.name());
      methodState.setReturnType(currentMethod.returnType().name());

      // Collect and set arguments in the MethodState
      List<VariableInfo> argumentInfos = extractArgumentsInfo(currentStackFrame, currentMethod);
      methodState.setArguments(argumentInfos);

      return methodState;
    } catch (EvaluateException e) {
      codeComparerUI.updateErrorDisplay("Current method could not be found!");
      throw new RuntimeException(e);
    } catch (ClassNotLoadedException e) {
      codeComparerUI.updateErrorDisplay(
          "Getting the return type of the current method was not possible");
      throw new RuntimeException(e);
    }
  }

  /**
   * Retrieves the return value of the last executed method in the debugger session.
   *
   * @param currentStackFrame The current stack frame in the debugger session.
   * @return A JSON representation of the return value, or a default message if no value exists.
   */
  public String getReturnValue(JavaStackFrame currentStackFrame) {
    LOGGER.warn("In getReturnValue");
    try {
      // Retrieve the last executed method and its return value
      Pair<Method, Value> methodValuePair =
          getDebugProcess(currentStackFrame).getLastExecutedMethod();
      if (methodValuePair != null) {
        Value returnValue = methodValuePair.getSecond();

        // Convert return value to JSON
        return extractVariableJson(currentStackFrame, returnValue);
      }
    } catch (Exception e) {
      codeComparerUI.updateErrorDisplay("Error collecting return value" + e.getMessage());
      return "Error collecting return value";
    }
    return "There is no return value for the last executed method";
  }

  /**
   * Checks if the given ObjectReference represents an exception.
   *
   * @param objectReference The object reference to check.
   * @return True if the object is an instance of Throwable, false otherwise.
   */
  private boolean isException(ObjectReference objectReference) {
    try {
      // Retrieve the object's runtime class and check if it or its superclass is Throwable
      Class<?> objectClass = Class.forName(objectReference.referenceType().name());
      while (objectClass != null) {
        if ("java.lang.Throwable".equals(objectClass.getTypeName())) {
          return true;
        }
        objectClass = objectClass.getSuperclass(); // Navigate to the superclass
      }
    } catch (Exception e) {
      LOGGER.error("Error while checking if object is an exception: " + e.getMessage(), e);
    }
    return false;
  }

  /**
   * Accesses the debug process associated with the current stack frame. Uses reflection to access a
   * private field in the JavaStackFrame class.
   *
   * @param javaStackFrame The current stack frame.
   * @return The DebugProcessImpl associated with the stack frame.
   */
  private DebugProcessImpl getDebugProcess(JavaStackFrame javaStackFrame) {
    try {
      // Use reflection to access the private "myDebugProcess" field in JavaStackFrame
      Class<?> clazz = javaStackFrame.getClass();
      java.lang.reflect.Field debugProcessField = clazz.getDeclaredField("myDebugProcess");
      debugProcessField.setAccessible(true);

      // Get the value of the field from the given instance
      return (DebugProcessImpl) debugProcessField.get(javaStackFrame);
    } catch (Exception e) {
      codeComparerUI.updateErrorDisplay("Failed to access myDebugProcess");
      throw new RuntimeException("Failed to access myDebugProcess", e);
    }
  }

  /**
   * Extracts information about method arguments, including their names, types, and values.
   *
   * @param currentStackFrame The current stack frame in the debugger session.
   * @param currentMethod The method being executed in the stack frame.
   * @return A list of VariableInfo objects, each representing an argument.
   */
  private List<VariableInfo> extractArgumentsInfo(
      JavaStackFrame currentStackFrame, Method currentMethod) {
    List<VariableInfo> argumentInfos = new ArrayList<>();

    try {
      // Get the list of method arguments
      List<LocalVariable> methodArguments = currentMethod.arguments();

      for (LocalVariable argument : methodArguments) {
        // Get the name of the argument
        String argumentName = argument.name();

        // Get the corresponding local variable in the stack frame
        LocalVariableProxyImpl argumentLocalVariable =
            currentStackFrame.getStackFrameProxy().visibleVariableByName(argumentName);
        Value argumentValue = stackFrame.getValue(argumentLocalVariable);

        // Convert the argument value to JSON and add it to the list
        String jsonRepresentation = extractVariableJson(currentStackFrame, argumentValue);
        argumentInfos.add(new VariableInfo(argumentName, jsonRepresentation));
      }
    } catch (AbsentInformationException e) {
      codeComparerUI.updateErrorDisplay("Getting Method Arguments was not possible");
      throw new RuntimeException(e);
    } catch (EvaluateException e) {
      codeComparerUI.updateErrorDisplay("Getting variable of Stack Frame was not possible");
      throw new RuntimeException(e);
    }
    return argumentInfos;
  }

  /**
   * Serializes a variable's value into JSON format.
   *
   * @param currentStackFrame The current stack frame.
   * @param value The value to serialize.
   * @return A JSON representation of the value.
   */
  private String extractVariableJson(JavaStackFrame currentStackFrame, Value value) {
    // Get the thread reference from the current stack frame
    ThreadReference threadReference =
        currentStackFrame.getStackFrameProxy().threadProxy().getThreadReference();

    // Convert the variable's value to JSON
    return ValueJsonSerializer.toJson(value, threadReference, new HashSet<>());
  }
}
