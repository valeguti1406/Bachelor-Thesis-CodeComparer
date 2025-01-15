package com.thesis.codecomparer.codeComparerDebugger;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

/**
 * Startup activity to configure debugging settings when a project is opened in IntelliJ IDEA.
 * Specifically, this class ensures that Java Exception Breakpoints are activated for the project.
 */
public class CodeComparerStartupActivity implements StartupActivity {

  private static final Logger LOGGER = Logger.getInstance(CodeComparerStartupActivity.class);

  /**
   * Runs after the project has been loaded and is fully initialized. Activates the Java Exception
   * Breakpoint to suspend the debugger on exceptions.
   *
   * @param project The project that was opened.
   */
  @Override
  public void runActivity(@NotNull Project project) {
    // Activate the Java Exception Breakpoint for this project
    ExceptionBreakpointEnabler.activateJavaAnyExceptionBreakpoint(project);

    // Log a message indicating that the breakpoint activation logic has been executed
    LOGGER.warn("Project loaded: Activated Java Exception Breakpoint.");
  }
}
