package com.thesis.codecomparer.codeComparerDebugger;

import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManagerListener;
import org.jetbrains.annotations.NotNull;

/**
 * Listener for the XDebuggerManager in IntelliJ.
 * This class listens for the start of new debugging sessions and adds a session-specific listener
 * to handle custom debugging behavior.
 */
public class DebuggerManagerListener implements XDebuggerManagerListener {

  /**
   * Invoked when a new debugging process starts.
   * Adds a custom session listener (`DebugSessionListener`) to the debugging session.
   *
   * @param debugProcess The debugging process that has started.
   */
  @Override
  public void processStarted(@NotNull final XDebugProcess debugProcess) {
    final XDebugSession debugSession = debugProcess.getSession();
    debugSession.addSessionListener(new DebugSessionListener(debugProcess));
  }
}
