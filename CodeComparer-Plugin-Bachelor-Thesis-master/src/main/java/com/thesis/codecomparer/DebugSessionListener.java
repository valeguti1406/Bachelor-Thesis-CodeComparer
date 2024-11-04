package com.thesis.codecomparer;

import com.intellij.debugger.engine.JavaStackFrame;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.*;
import org.jetbrains.annotations.NotNull;

public class DebugSessionListener implements XDebugSessionListener {

  private static final Logger LOGGER = Logger.getInstance(DebugSessionListener.class);
  private static final String CONTENT_ID = "com.thesis.CodeComparer";
  private static final String TOOLBAR_ACTION =
      "CodeComparer.VisualizerToolbar"; // group id defined in plugin.xml

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
  public void sessionStopped() {
    LOGGER.warn("Debugger stoped");
  }

  @Override
  public void sessionPaused() {
    LOGGER.warn("Debugger paused");
    this.getBreakpointStates();
  }

  private void getBreakpointStates() {

    StackFrameProxyImpl stackFrame = getStackFrameProxy();

    BreakpointStateCollector breakpointStateCollector = new BreakpointStateCollector(stackFrame, 3);
    //String collectedState = breakpointStateCollector.analyzeStackFrame();
    // displayStateInPanel(collectedState);

    // TODO: how to do step into and step out without generating cascade of in and out (just once)
    // now we are on the breakpoint line that has the method/Library call
    // we need to step into it, to get the method information and then step out
    /*LOGGER.warn("Trying to step into");
    //To ensure that it runs on the EDT
    ApplicationManager.getApplication().invokeLater(debugSession::stepInto);
    LOGGER.warn("After step into");*/

    JavaStackFrame javaStackFrame = (JavaStackFrame) debugSession.getCurrentStackFrame();
    if (javaStackFrame != null) {
      StringBuilder infoToDisplay = new StringBuilder();
      // get fileName and current line and save states in file
      if (debugSession.getCurrentPosition() != null) {
        String fileName = debugSession.getCurrentPosition().getFile().getNameWithoutExtension();
        int line = debugSession.getCurrentPosition().getLine() + 1;
        infoToDisplay.append("File name: ").append(fileName).append("\n");
        infoToDisplay.append("Line: ").append(line).append("\n\n");
        //saveStateToFile(collectedState, fileName, line);
      }
      infoToDisplay.append(breakpointStateCollector.getMethodInfo(javaStackFrame));
      displayStateInPanel(infoToDisplay.toString());
    }

    /*
    LOGGER.warn("Trying to step out");
    ApplicationManager.getApplication().invokeLater(debugSession::stepOut);
    LOGGER.warn("After to step out");
    */
  }

  @NotNull private StackFrameProxyImpl getStackFrameProxy() {
    JavaStackFrame currentStackFrame = (JavaStackFrame) debugSession.getCurrentStackFrame();
    if (currentStackFrame == null) {
      LOGGER.warn("Current stack frame could not be found!"); // TODO: create error class
    }

    return currentStackFrame.getStackFrameProxy();
  }

  private void displayStateInPanel(String stateInfo) {
    // Clear the current UI content
    userInterface.removeAll();

    // Create a text area to display the state information
    JTextArea stateTextArea = new JTextArea(stateInfo);
    stateTextArea.setEditable(false); // Read-only display
    JScrollPane scrollPane = new JScrollPane(stateTextArea);

    // Add the scroll pane to the main panel
    userInterface.add(scrollPane, BorderLayout.CENTER);

    // Refresh the UI to ensure changes are visible
    userInterface.revalidate();
    userInterface.repaint();
  }

  /**
   * Add "Start CodeComparer" tab to the debugging window When the tab is clicked
   * StartCodeComparer.actionPerformed is called
   */
  private void initUI() {
    if (this.userInterface != null) {
      return;
    }
    this.userInterface = new JPanel();
    userInterface.setLayout(new BorderLayout());
    final var uiContainer = new SimpleToolWindowPanel(false, true);

    // create tab and connect it to StartCodeComparer
    final var actionManager = ActionManager.getInstance();
    final var actionToolbar =
        actionManager.createActionToolbar(
            TOOLBAR_ACTION, (DefaultActionGroup) actionManager.getAction(TOOLBAR_ACTION), false);
    actionToolbar.setTargetComponent(this.userInterface);
    uiContainer.setToolbar(actionToolbar.getComponent());
    uiContainer.setContent(this.userInterface);

    // add tab to the debugging ui
    final RunnerLayoutUi ui = this.debugSession.getUI();
    final var content =
        ui.createContent(
            CONTENT_ID, uiContainer, "CodeComparer", CodeComparerIcons.DIFF_ICON, null);
    content.setCloseable(false);
    UIUtil.invokeLaterIfNeeded(() -> ui.addContent(content));
  }

  //TODO: make file saving work
  private void saveStateToFile(String state, String fileName, int line) {
    String directoryPath = "CodeComparer-Plugin/output";
    File outputDir = new File(directoryPath);
    if (!outputDir.exists()) {
      boolean dirCreated = outputDir.mkdirs(); // Create the directory if it doesn't exist
      LOGGER.warn("Created directory? " + dirCreated);
    }

    String filePath = directoryPath + "/collected_states.txt";
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
      writer.write("File: " + fileName + ", Line: " + line + "\n");
      writer.write(state);
      writer.write("\n====================\n"); // Separate different breakpoints
      LOGGER.warn("Successfully saved collected state to file: " + filePath);

      // TODO: states seemed to be saved in file and directory created but where?
    } catch (IOException e) {
      LOGGER.error("Error saving collected state to file", e);
    }
  }
}
