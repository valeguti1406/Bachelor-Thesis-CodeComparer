package com.thesis.codecomparer;

import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManagerListener;
import org.jetbrains.annotations.NotNull;

public class DebuggerManagerListener implements XDebuggerManagerListener {

  /** Listens to new Debugging Sessions and adds to them a session Listener */
  @Override
  public void processStarted(@NotNull final XDebugProcess debugProcess) {
    final XDebugSession debugSession = debugProcess.getSession();
    debugSession.addSessionListener(new DebugSessionListener(debugProcess));
  }
}
