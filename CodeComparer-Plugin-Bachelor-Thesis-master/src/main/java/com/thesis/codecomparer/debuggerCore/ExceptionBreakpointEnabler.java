package com.thesis.codecomparer.debuggerCore;

import com.intellij.debugger.ui.breakpoints.JavaExceptionBreakpointType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import org.jetbrains.java.debugger.breakpoints.properties.JavaExceptionBreakpointProperties;

/**
 * Utility class to manage and activate Java Exception Breakpoints in the IntelliJ IDEA debugger.
 */
public class ExceptionBreakpointEnabler {

  private static final Logger LOGGER = Logger.getInstance(ExceptionBreakpointEnabler.class);

  /**
   * Activates the Java Exception Breakpoint for "Any Exception". This method ensures the breakpoint
   * will suspend the debugger on uncaught exceptions.
   *
   * @param project The current project in which the debugger is running.
   */
  public static void activateJavaUncaughtExceptionBreakpoint(Project project) {
    // Access the XDebuggerManager, which manages debugging sessions and breakpoints for the project
    XDebuggerManager debuggerManager = XDebuggerManager.getInstance(project);

    // Get the XBreakpointManager, which provides access to all breakpoints in the current project
    XBreakpointManager breakpointManager = debuggerManager.getBreakpointManager();

    // Iterate over all breakpoints to locate a Java Exception Breakpoint
    for (XBreakpoint<?> breakpoint : breakpointManager.getAllBreakpoints()) {
      // Check if the breakpoint is of type JavaExceptionBreakpointType
      if (breakpoint.getType() instanceof JavaExceptionBreakpointType) {
        // Retrieve the properties of the Java Exception Breakpoint
        JavaExceptionBreakpointProperties properties =
            (JavaExceptionBreakpointProperties) breakpoint.getProperties();
        if (properties != null) {
          // Configure the breakpoint to not notify on caught exceptions
          properties.NOTIFY_CAUGHT = false;
          // Configure the breakpoint to notify on uncaught exceptions
          properties.NOTIFY_UNCAUGHT = true;
          // Enable the breakpoint in the debugger
          breakpoint.setEnabled(true);
        }
        // Exit the loop after activating the first matching Java Exception Breakpoint
        LOGGER.warn("Java Exception Breakpoint activated.");
        return;
      }
    }

    // Log a message if no Java Exception Breakpoint was found to activate
    LOGGER.warn("No Java Exception Breakpoint found to activate.");
  }
}
