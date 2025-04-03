package com.thesis.codecomparer.debuggerCore;

import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.JavaDebugProcess;
import com.intellij.debugger.engine.JavaStackFrame;
import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.impl.DebuggerContextImpl;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.jdi.LocalVariableProxyImpl;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.intellij.xdebugger.XDebugSession;
import com.sun.jdi.*;
import com.sun.jdi.event.ExceptionEvent;
import com.thesis.codecomparer.dataModels.ExceptionDetails;
import com.thesis.codecomparer.dataModels.MethodState;
import com.thesis.codecomparer.dataModels.ParameterInfo;
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
   * return type. - Parameter names and serialized JSON representations of their values.
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

      // Collect and set parameters in the MethodState
      List<ParameterInfo> parameterInfo = extractParametersInfo(currentStackFrame, currentMethod);
      methodState.setParameters(parameterInfo);

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
   * Extracts information about method parameters, including their names, types, and values.
   *
   * @param currentStackFrame The current stack frame in the debugger session.
   * @param currentMethod The method being executed in the stack frame.
   * @return A list of ParameterInfo objects, each representing a parameter.
   */
  private List<ParameterInfo> extractParametersInfo(
      JavaStackFrame currentStackFrame, Method currentMethod) {
    List<ParameterInfo> parameterInfos = new ArrayList<>();

    try {
      // Get the list of method parameters
      List<LocalVariable> methodParameters = currentMethod.arguments();

      for (LocalVariable parameter : methodParameters) {
        // Get the name of the parameter
        String parameterName = parameter.name();

        // Get the corresponding local variable in the stack frame
        LocalVariableProxyImpl parameterLocalVariable =
            currentStackFrame.getStackFrameProxy().visibleVariableByName(parameterName);
        Value parameterValue = stackFrame.getValue(parameterLocalVariable);

        // Convert the parameter value to JSON and add it to the list
        String jsonRepresentation = extractVariableJson(currentStackFrame, parameterValue);
        parameterInfos.add(new ParameterInfo(parameterName, jsonRepresentation));
      }
    } catch (AbsentInformationException e) {
      codeComparerUI.updateErrorDisplay("Getting Method Parameters was not possible");
      throw new RuntimeException(e);
    } catch (EvaluateException e) {
      codeComparerUI.updateErrorDisplay("Getting variable of Stack Frame was not possible");
      throw new RuntimeException(e);
    }
    return parameterInfos;
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

  /**
   * Processes a Java exception breakpoint hit during a debug session. Extracts exception details if
   * any exception events are found.
   *
   * @param javaStackFrame the current Java stack frame
   * @param debugSession the active debug session
   * @return an ExceptionDetails object if an exception was found; otherwise null
   */
  public ExceptionDetails processJavaExceptionBreakpoint(
      JavaStackFrame javaStackFrame, XDebugSession debugSession) {

    // Create evaluation context for the current debug session
    EvaluationContextImpl evalContext = createEvaluationContext(debugSession);
    if (evalContext != null) {
      SuspendContextImpl suspendContext = evalContext.getSuspendContext();

      // Collect all exception events that occurred in the current suspend context
      List<ExceptionEvent> exceptionEvents = collectExceptionEvents(suspendContext);

      // If at least one exception was caught, process it
      if (!exceptionEvents.isEmpty()) {
        ExceptionEvent exceptionEvent = exceptionEvents.get(0);
        ObjectReference exceptionObject = exceptionEvent.exception();
        return processExceptionObject(javaStackFrame, exceptionObject);
      }
    }
    return null;
  }

  // Creates the EvaluationContextImpl for the given DebugSession
  private EvaluationContextImpl createEvaluationContext(XDebugSession debugSession) {
    JavaDebugProcess javaDebugProcess = (JavaDebugProcess) debugSession.getDebugProcess();
    DebuggerSession debuggerSession = javaDebugProcess.getDebuggerSession();
    DebuggerContextImpl context = debuggerSession.getContextManager().getContext();

    // Return an evaluation context that allows inspecting variables and method calls
    return context.createEvaluationContext();
  }

  // Collects ExceptionEvent objects from the SuspendContext
  private List<ExceptionEvent> collectExceptionEvents(SuspendContextImpl suspendContext) {
    return DebuggerUtilsEx.getEventDescriptors(suspendContext).stream()
        .map(pair -> pair.getSecond()) // Get the event from each pair
        .filter(event -> event instanceof ExceptionEvent) // Keep only exception events
        .map(event -> (ExceptionEvent) event)
        .toList();
  }

  // Processes the ExceptionObject, extracting its details and stack trace
  private ExceptionDetails processExceptionObject(
      JavaStackFrame javaStackFrame, ObjectReference exceptionObject) {
    try {
      // Extract basic information from the exception
      String exceptionType = exceptionObject.referenceType().name();
      String exceptionMessage = extractExceptionMessage(exceptionObject);
      String stackTrace = extractStackTrace(javaStackFrame, exceptionObject);

      // Create a new ExceptionDetails object to store exception details
      ExceptionDetails exceptionInfo = new ExceptionDetails();
      exceptionInfo.setExceptionType(exceptionType);
      exceptionInfo.setExceptionMessage(exceptionMessage);
      exceptionInfo.setStackTrace(stackTrace);

      // Update UI with exception summary
      codeComparerUI.updateErrorDisplay(
          "Java Exception thrown: " + exceptionInfo.getExceptionType());

      // Save exception info in the BreakpointState
      return exceptionInfo;
    } catch (Exception e) {
      codeComparerUI.updateErrorDisplay("Error processing exception object:" + e);
    }
    return null;
  }

  // Extracts the stack trace from the Throwable object
  private String extractStackTrace(JavaStackFrame javaStackFrame, ObjectReference exceptionObject)
      throws Exception {
    // Force initialization of the 'stackTrace' field by calling getStackTrace()
    ThreadReference threadReference =
        javaStackFrame.getStackFrameProxy().threadProxy().getThreadReference();
    Method getStackTraceMethod =
        exceptionObject.referenceType().methodsByName("getStackTrace").get(0);

    if (getStackTraceMethod != null) {
      exceptionObject.invokeMethod(
          threadReference, getStackTraceMethod, Collections.emptyList(), 0);
      Field stackTraceField = exceptionObject.referenceType().fieldByName("stackTrace");

      if (stackTraceField != null) {
        Value stackTraceValue = exceptionObject.getValue(stackTraceField);
        // If stack trace is an array, process each element
        if (stackTraceValue instanceof ArrayReference arrayRef) {
          return processStackTraceArray(arrayRef, threadReference);
        }
      }
    }
    return null;
  }

  // Processes the stack trace array to retrieve its toString representation
  private String processStackTraceArray(ArrayReference arrayRef, ThreadReference threadReference)
      throws Exception {
    StringBuilder stackTraceBuilder = new StringBuilder();

    // Iterate over each element in the array and convert to string
    for (Value element : arrayRef.getValues()) {
      if (element instanceof ObjectReference objRef) {
        Method toStringMethod = objRef.referenceType().methodsByName("toString").get(0);

        if (toStringMethod != null) {
          Value result =
              objRef.invokeMethod(threadReference, toStringMethod, Collections.emptyList(), 0);
          if (result instanceof StringReference) {
            stackTraceBuilder.append(((StringReference) result).value()).append("\n");
          }
        }
      }
    }
    return stackTraceBuilder.toString();
  }

  // Extracts the exception message from the Throwable object
  private String extractExceptionMessage(ObjectReference exceptionObject) {
    Field messageField = exceptionObject.referenceType().fieldByName("detailMessage");
    if (messageField != null) {
      Value messageValue = exceptionObject.getValue(messageField);
      if (messageValue instanceof StringReference) {
        return ((StringReference) messageValue).value();
      }
    }
    return null;
  }
}
