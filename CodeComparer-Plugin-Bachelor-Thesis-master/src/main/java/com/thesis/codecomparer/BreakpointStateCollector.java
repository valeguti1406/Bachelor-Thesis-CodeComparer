package com.thesis.codecomparer;

import com.intellij.debugger.engine.JavaStackFrame;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.jdi.LocalVariableProxyImpl;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.openapi.diagnostic.Logger;
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
    LOGGER.warn("in getMethodInfo");
    StringBuilder sb = new StringBuilder("------- Method info: ---------");
    sb.append("\n");
    try {
      Method currentMethod = currentStackFrame.getStackFrameProxy().location().method();
      LOGGER.warn("method name : " + currentMethod.name());
      sb.append("name: ").append(currentMethod.name()).append("\n");

      Type returnType = currentMethod.returnType();
      LOGGER.warn("Return Type: " + returnType);
      sb.append("Return Type: ").append(returnType).append("\n");

      // I just get the method signature, and not the variable that is getting returned, so name and value don't make sense here
      //TODO: get the return value
      /*if (! returnType.toString().equals("void")) {
        String returnName = returnType.name();
        LOGGER.warn("Return Name: " + returnName);
        sb.append("Return Name: ").append(returnName).append("\n");

        LOGGER.warn("Trying to get value from stackframe ");
        LocalVariableProxyImpl returnVariable =
            currentStackFrame.getStackFrameProxy().visibleVariableByName(returnName);
        LOGGER.warn("Local return variable  " + returnVariable);
        LOGGER.warn("Getting eturn Value");
        Value returnValue = stackFrame.getValue(returnVariable);
        LOGGER.warn("Return Value: " + returnValue);
        sb.append("Return Value: ").append(returnValue).append("\n");
      }*/

      extractArgumentsInfo(currentStackFrame,currentMethod,sb);

      return sb.toString();
    } catch (EvaluateException e) {
      LOGGER.warn("Current method could not be found!");
      throw new RuntimeException(e);
    } catch (ClassNotLoadedException e) {
      LOGGER.warn("Getting the return type of the current method was not possible");
        throw new RuntimeException(e);
    }
  }

  /**
   * Get type, name and value of the arguments of a method and add this information to the string builder
   */
  private StringBuilder extractArgumentsInfo (JavaStackFrame currentStackFrame, Method currentMethod, StringBuilder sb){
    try {
      List<LocalVariable> methodArguments = currentMethod.arguments();
      for (LocalVariable argument : methodArguments) {

        Type argumentTYpe = argument.type();
        LOGGER.warn("Parameter Type:" + argumentTYpe);
        sb.append("Parameter Type: ").append(argumentTYpe).append("\n");

        String argumentName = argument.name();
        LOGGER.warn("Parameter Name: " + argumentName);
        sb.append("Parameter Name: ").append(argumentName).append("\n");

        LocalVariableProxyImpl argumentLocalVariable =
                currentStackFrame.getStackFrameProxy().visibleVariableByName(argumentName);
        Value argumentValue = stackFrame.getValue(argumentLocalVariable);
        LOGGER.warn("Parameter Value: " + argumentValue);
        sb.append("Parameter Value: ").append(argumentValue).append("\n");
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
    return sb;
  }

  // Analyze the 'this' object in the stack frame
  private void analyzeThisObject(@NotNull StackFrameProxyImpl stackFrame) throws EvaluateException {
    final ObjectReference thisObjectReference = stackFrame.thisObject();
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
        .append("Exploring object: ")
        .append(objectName)
        .append(", of type: ")
        .append(objectType)
        .append("\n");

    if (objectReference instanceof ArrayReference arrayReference) {
      this.appendArrayValues(arrayReference, remainingDepth);
      return;
    }

    // Get and iterate over all fields of the object
    Map<Field, Value> fields =
        objectReference.getValues(objectReference.referenceType().allFields());
    for (Map.Entry<Field, Value> entry : fields.entrySet()) {
      String fieldName = entry.getKey().name();

      // Append information about each field
      stateInfoBuilder
          .append("Field: ")
          .append(fieldName)
          .append(", of type: ")
          .append(entry.getKey().typeName())
          .append("\n");
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
      final Value variableValue, String variableName, int remainingDepthToBeExplored) {
    if (variableValue == null) {
      stateInfoBuilder.append(" is null\n");
    } else if (variableValue instanceof PrimitiveValue) {
      stateInfoBuilder.append(", with value: ").append(variableValue).append("\n");
    } else if (variableValue instanceof StringReference stringReference) {
      stateInfoBuilder.append(", with value: ").append(stringReference.value()).append("\n");
    } else { // If the field is an object reference, explore it recursively
      final ObjectReference obj = (ObjectReference) variableValue;
      this.exploreObject(obj, variableName, remainingDepthToBeExplored);
    }
  }
}
