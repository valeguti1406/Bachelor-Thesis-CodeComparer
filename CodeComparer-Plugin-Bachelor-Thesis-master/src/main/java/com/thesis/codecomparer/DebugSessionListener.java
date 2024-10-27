package com.thesis.codecomparer;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.util.ui.UIUtil;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.thesis.codecomparer.ui.CodeComparerIcons;
import java.awt.*;
import javax.swing.*;
import org.jetbrains.annotations.NotNull;

public class DebugSessionListener implements XDebugSessionListener {

  private static final Logger LOGGER = Logger.getInstance(DebugSessionListener.class);
  private static final String CONTENT_ID = "com.thesis.CodeComparer";
  private static final String TOOLBAR_ACTION = "CodeComparer.VisualizerToolbar"; // group id defined in plugin.xml

  private JPanel userInterface;

  private final XDebugSession debugSession;

  public DebugSessionListener(@NotNull XDebugProcess debugProcess) {
    this.debugSession = debugProcess.getSession();
    debugProcess
        .getProcessHandler()
        .addProcessListener(
            new ProcessListener() {
              @Override
              public void startNotified(@NotNull ProcessEvent event) {
                DebugSessionListener.this.initUI();
              }
            });
  }

  @Override
  public void sessionPaused() {
    LOGGER.warn("In sessionPaused");
    this.startVisualDebugging();
  }

  private void startVisualDebugging() {

    /*TODO: save var states*/

    //get fileNams and current line TODO: save them
    if (debugSession.getCurrentPosition() != null) {
      String fileName = debugSession.getCurrentPosition().getFile().getNameWithoutExtension();
      int line = debugSession.getCurrentPosition().getLine() + 1;
    }
  }

  /**
   * Add "Start CodeComparer" tab to the debugging window
   * When the tab is clicked StartCodeComparer.actionPerformed is called
   */
  private void initUI() {
    if (this.userInterface != null) {return;}
    this.userInterface = new JPanel();
    userInterface.setLayout(new BorderLayout());
    final var uiContainer = new SimpleToolWindowPanel(false, true);

    //create tab and connect it to StartCodeComparer
    final var actionManager = ActionManager.getInstance();
    final var actionToolbar =
        actionManager.createActionToolbar(
            TOOLBAR_ACTION, (DefaultActionGroup) actionManager.getAction(TOOLBAR_ACTION), false);
    actionToolbar.setTargetComponent(this.userInterface);
    uiContainer.setToolbar(actionToolbar.getComponent());
    uiContainer.setContent(this.userInterface);

    //add tab to the debugging ui
    final RunnerLayoutUi ui = this.debugSession.getUI();
    final var content =
        ui.createContent(
            CONTENT_ID, uiContainer, "Start CodeComparer", CodeComparerIcons.DIFF_ICON, null);
    content.setCloseable(false);
    UIUtil.invokeLaterIfNeeded(() -> ui.addContent(content));
  }

}
