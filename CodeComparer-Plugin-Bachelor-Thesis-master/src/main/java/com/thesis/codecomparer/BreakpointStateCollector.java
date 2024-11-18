package com.thesis.codecomparer;

import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.JavaStackFrame;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.jdi.LocalVariableProxyImpl;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.sun.jdi.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class BreakpointStateCollector {

  // StringBuilder used to collect state information for displaying
  private final StringBuilder stateInfoBuilder;
  private static final Logger LOGGER = Logger.getInstance(BreakpointStateCollector.class);

  // Represents the current stack frame to analyze
  private final StackFrameProxyImpl stackFrame;
  private final Set<Long>
      seenObjectIds; // Set to keep track of visited objects to avoid re-analysis (handle circular
  // references)
  private final int loadingDepth; // Depth limit for traversing nested objects

  public BreakpointStateCollector(@NotNull StackFrameProxyImpl stackFrame, int loadingDepth) {
    this.stackFrame = stackFrame;
    this.loadingDepth = loadingDepth;
    this.seenObjectIds = new HashSet<>();
    this.stateInfoBuilder = new StringBuilder();
  }

  // Main method to analyze the current stack frame and return the collected information as a string
  public String analyzeStackFrame() {
    try {
      // Analyze the 'this' object if present
      this.analyzeThisObject(this.stackFrame);
      // Analyze all local variables in the current scope
      this.analyzeVariablesInScope(this.stackFrame);
      // Clear tracked objects after analysis is complete
      seenObjectIds.clear();
      return stateInfoBuilder.toString();
    } catch (EvaluateException e) {
      LOGGER.error("Error analyzing stack frame", e);
      return "Failed to analyze stack frame.";
    }
  }

  /*
   Extract from the Method (Library Call), the inputs and outputs
  */
  public String getMethodInfo(@NotNull JavaStackFrame currentStackFrame) {
    stateInfoBuilder.append("------- Method info: ---------").append("\n");
    try {
      Method currentMethod = currentStackFrame.getStackFrameProxy().location().method();
      stateInfoBuilder.append("Name: ").append(currentMethod.name()).append("\n");

      Type returnType = currentMethod.returnType();
      stateInfoBuilder.append("Return Type: ").append(returnType).append("\n").append("\n");

      extractArgumentsInfo(currentStackFrame, currentMethod);

      return stateInfoBuilder.toString();
    } catch (EvaluateException e) {
      LOGGER.warn("Current method could not be found!");
      throw new RuntimeException(e);
    } catch (ClassNotLoadedException e) {
      LOGGER.warn("Getting the return type of the current method was not possible");
      throw new RuntimeException(e);
    }
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

  public String getReturnValue(JavaStackFrame javaStackFrame) {
    LOGGER.warn("In getReturnValue");
    Pair<Method, Value> methodValuePair = getDebugProcess(javaStackFrame).getLastExecutedMethod();
    if (methodValuePair != null) {
      Value returnValue = methodValuePair.getSecond();
      stateInfoBuilder.append("returnValue: ").append("\n");
      appendValue(returnValue, "returnValue", 2);
    }
    return stateInfoBuilder.toString();
  }

  /**
   * Get type, name and value of the arguments of a method and add this information to the string
   * builder
   */
  private void extractArgumentsInfo(JavaStackFrame currentStackFrame, Method currentMethod) {
    try {
      List<LocalVariable> methodArguments = currentMethod.arguments();
      if (!methodArguments.isEmpty()) {
        stateInfoBuilder.append("--- Method Arguments: ---").append("\n");
      }
      for (LocalVariable argument : methodArguments) {

        String argumentName = argument.name();
        LocalVariableProxyImpl argumentLocalVariable =
            currentStackFrame.getStackFrameProxy().visibleVariableByName(argumentName);
        Value argumentValue = stackFrame.getValue(argumentLocalVariable);

        extractVariableInfo(argumentValue, argument.name(), argument.type(), 3);
      }
    } catch (AbsentInformationException e) {
      LOGGER.warn("Getting Method Arguments was not possible");
      throw new RuntimeException(e);
    } catch (ClassNotLoadedException e) {
      LOGGER.warn("Getting Argument type was not possible");
      throw new RuntimeException(e);
    } catch (EvaluateException e) {
      LOGGER.warn("Getting variable of Stack Frame was not possible");
      throw new RuntimeException(e);
    }
  }

  // Analyze the 'this' object in the stack frame
  private void analyzeThisObject(@NotNull StackFrameProxyImpl stackFrame) {
    final ObjectReference thisObjectReference;
    try {
      thisObjectReference = stackFrame.thisObject();
    } catch (EvaluateException e) {
      LOGGER.warn("Getting the 'this' object was not possible");
      throw new RuntimeException(e);
    }
    if (thisObjectReference == null) {
      stateInfoBuilder.append("this object was null!\n");
      return;
    }
    // Explore fields and nested objects of 'this'
    this.exploreObject(thisObjectReference, "this", loadingDepth);
  }

  // Analyze all variables that are in scope for the given stack frame
  private void analyzeVariablesInScope(@NotNull StackFrameProxyImpl stackFrame)
      throws EvaluateException {

    List<LocalVariableProxyImpl> methodVariables = stackFrame.visibleVariables();
    for (LocalVariableProxyImpl localVariable : methodVariables) {
      Value variableValue = stackFrame.getValue(localVariable);
      String variableName = localVariable.name();
      String variableType = localVariable.typeName();
      // Append information about the local variable
      stateInfoBuilder
          .append("Variable: ")
          .append(variableName)
          .append(", of type: ")
          .append(variableType);
      this.appendValue(variableValue, variableName, loadingDepth);
    }
  }

  // Recursively explore an object's fields and nested objects, up to the specified depth
  private void exploreObject(
      @NotNull ObjectReference objectReference, String objectName, int remainingDepth) {
    // Stop if the depth limit is reached or the object has already been analyzed
    if (remainingDepth < 0 || seenObjectIds.contains(objectReference.uniqueID())) {
      return;
    }

    // Mark the object as seen to avoid re-analysis
    seenObjectIds.add(objectReference.uniqueID());

    // Get object type and append information
    String objectType = objectReference.referenceType().name();
    stateInfoBuilder
        .append("Name: ")
        .append(objectName)
        .append(", of type: ")
        .append(objectType)
        .append("\n")
        .append("\n");

    if (objectReference instanceof ArrayReference arrayReference) {
      this.appendArrayValues(arrayReference, remainingDepth);
      return;
    }

    // Get and iterate over all fields of the object
    Map<Field, Value> fields =
        objectReference.getValues(objectReference.referenceType().allFields());
    if (!fields.isEmpty()) {
      stateInfoBuilder.append("--- Object fields: ---").append("\n");
    }
    for (Map.Entry<Field, Value> entry : fields.entrySet()) {
      String fieldName = entry.getKey().name();

      // Append information about each field
      stateInfoBuilder
          .append("Field: ")
          .append(fieldName)
          .append(", of type: ")
          .append(entry.getKey().typeName());
      this.appendValue(entry.getValue(), fieldName, remainingDepth - 1);
    }
  }

  private void appendArrayValues(final ArrayReference arrayRef, int remainingDepthToBeExplored) {
    stateInfoBuilder.append("Array values: \n");
    for (int i = 0; i < arrayRef.length(); i++) {
      final Value value = arrayRef.getValue(i);
      final String variableName = String.valueOf(i);
      stateInfoBuilder.append("in position: ").append(i);
      this.appendValue(value, variableName, remainingDepthToBeExplored);
    }
  }

  private void appendValue(
      final Value variableValue, final String variableName, int remainingDepthToBeExplored) {
    if (variableValue == null) {
      stateInfoBuilder.append(" is null\n");
    } else if (variableValue instanceof PrimitiveValue) {
      stateInfoBuilder.append(", with value: ").append(variableValue).append("\n\n");
    } else if (variableValue instanceof StringReference stringReference) {
      stateInfoBuilder.append(", with value: ").append(stringReference.value()).append("\n\n");
    } else { // If the field is an object reference, explore it recursively
      final ObjectReference obj = (ObjectReference) variableValue;
      this.exploreObject(obj, variableName, remainingDepthToBeExplored);
    }
  }

  private void extractVariableInfo(
      final Value variableValue,
      final String variableName,
      final Type type,
      int remainingDepthToBeExplored) {
    if (variableValue == null) {
      stateInfoBuilder.append("This variable is null\n");
    } else if (variableValue instanceof PrimitiveValue) {
      stateInfoBuilder
          .append("Name: ")
          .append(variableName)
          .append(", with type: ")
          .append(type)
          .append(", with value: ")
          .append(variableValue)
          .append("\n\n");
    } else if (variableValue instanceof StringReference stringReference) {
      stateInfoBuilder
          .append("Name: ")
          .append(variableName)
          .append(", with type: ")
          .append(type)
          .append(", with value: ")
          .append(stringReference.value())
          .append("\n\n");
    } else { // If the field is an object reference, explore it recursively
      final ObjectReference obj = (ObjectReference) variableValue;
      this.exploreObject(obj, variableName, remainingDepthToBeExplored);
    }
  }
}
