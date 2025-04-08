package com.thesis.codecomparer.debuggerCore;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A startup activity that runs automatically after a project is fully loaded in IntelliJ.
 * This version implements the modern coroutine-based ProjectActivity interface,
 * compatible with the latest IntelliJ Platform SDKs (2022.3+).
 */
public class CodeComparerStartupActivity implements ProjectActivity {

  private static final Logger LOGGER = Logger.getInstance(CodeComparerStartupActivity.class);

    /**
     * This method is automatically called by IntelliJ after the project has been initialized.
     * It runs as a coroutine (suspend function in Kotlin), which is why it uses a Continuation parameter.
     *
     * @param project The current IntelliJ project.
     * @param continuation Internal Kotlin continuation mechanism for coroutines (automatically handled by IntelliJ).
     * @return Returns Kotlin's Unit.INSTANCE to signal completion.
     */
  @Nullable
  @Override
  public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
      // Activate the Java Exception Breakpoint for this project
      ExceptionBreakpointEnabler.activateJavaUncaughtExceptionBreakpoint(project);

      // Log a message indicating that the breakpoint activation logic has been executed
      LOGGER.warn("Project loaded: Activated Java Exception Breakpoint.");

      // Return Kotlin's 'Unit' to indicate successful execution
      return Unit.INSTANCE;
  }
}
