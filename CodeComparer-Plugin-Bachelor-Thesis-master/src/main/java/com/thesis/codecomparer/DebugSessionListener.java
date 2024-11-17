package com.thesis.codecomparer;

import com.intellij.debugger.engine.JavaStackFrame;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
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
import java.util.concurrent.CountDownLatch;
import javax.swing.*;
import org.jetbrains.annotations.NotNull;

public class DebugSessionListener implements XDebugSessionListener {

  private static final Logger LOGGER = Logger.getInstance(DebugSessionListener.class);
  private static final String CONTENT_ID = "com.thesis.CodeComparer";
  private static final String TOOLBAR_ACTION =
      "CodeComparer.VisualizerToolbar"; // group id defined in plugin.xml

  private JPanel userInterface;

  private final XDebugSession debugSession;

  private final String outputDirectoryPath = "CodeComparer-Plugin/output";
  private final String outputFileName = "collected_states.txt";
  private File outputFile;

  private boolean stepedOver = false;


  public DebugSessionListener(@NotNull XDebugProcess debugProcess) {
    createOutputFile();
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
    LOGGER.warn("Debugger paused");
    this.getBreakpointStates();
  }

  @Override
  public void sessionResumed() {
    LOGGER.warn("Debugger resumed");
  }

  @Override
  public void sessionStopped() {
    LOGGER.warn("Debugger stopped");
  }


  private void createOutputFile() {
    String directoryPath = outputDirectoryPath;
    File outputDir = new File(directoryPath);
    if (!outputDir.exists()) {
      boolean dirCreated = outputDir.mkdirs(); // Create the directory if it doesn't exist
      LOGGER.warn("Created directory? " + dirCreated);
    }

    String filePath = directoryPath + "/" + outputFileName;
    outputFile = new File(filePath);

    // Empty the file if it exists
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, false))) {
      // Empty the file by writing nothing
      writer.write("");
      LOGGER.warn("Emptied the collected states file: " + outputFile.getAbsolutePath());
    } catch (IOException e) {
      LOGGER.error("Error emptying the collected state file", e);
    }
  }

  private void getBreakpointStates() {

    StackFrameProxyImpl stackFrame = getStackFrameProxy();

    BreakpointStateCollector breakpointStateCollector = new BreakpointStateCollector(stackFrame, 3);

    JavaStackFrame javaStackFrame = (JavaStackFrame) debugSession.getCurrentStackFrame();
    if (javaStackFrame != null) {
      StringBuilder infoToDisplay = new StringBuilder();
      // get fileName and current line and save states in file
      if (debugSession.getCurrentPosition() != null) {
        String fileName = debugSession.getCurrentPosition().getFile().getNameWithoutExtension();
        int line = debugSession.getCurrentPosition().getLine() + 1;
        infoToDisplay.append("File name: ").append(fileName).append("\n");
        infoToDisplay.append("Line: ").append(line).append("\n\n");
      }

      //TODO: debugging settings including return type

      infoToDisplay.append(breakpointStateCollector.getMethodInfo(javaStackFrame)).append("\n\n");
      infoToDisplay.append(breakpointStateCollector.getReturnValue(javaStackFrame)).append("\n\n");
      infoToDisplay.append(saveStateToFile(infoToDisplay.toString()));
      displayStateInPanel(infoToDisplay.toString());

      // TODO: automatically step out and then resume program

      /*if (!stepedOver) {
        infoToDisplay.append(breakpointStateCollector.getMethodInfo(javaStackFrame)).append("\n\n");
        displayStateInPanel(infoToDisplay.toString());
        LOGGER.warn("Trying to step over");

        // Create a CountDownLatch
        CountDownLatch latch = new CountDownLatch(1);

        // Add a listener for when the stack frame changes
        debugSession.addSessionListener(new XDebugSessionListener() {
          @Override
          public void stackFrameChanged() {
            LOGGER.warn("Step over completed");

            // Perform your post-stepOver logic here
            infoToDisplay.append(breakpointStateCollector.getReturnValue(javaStackFrame)).append("\n\n");
            infoToDisplay.append(saveStateToFile(infoToDisplay.toString()));
            displayStateInPanel(infoToDisplay.toString());

            // Decrement the latch to unblock waiting thread
            latch.countDown();

            // Remove the listener to avoid duplicate calls
            debugSession.removeSessionListener(this);
            LOGGER.warn("Listener removed");
          }
        });

        // Trigger the stepOver in a separate thread to avoid blocking the debugger's event loop
        ApplicationManager.getApplication().invokeLater(() -> {
          LOGGER.warn("Invoking stepOver");
          debugSession.stepOver(true);
          LOGGER.warn("stepOver invoked");
        });

        // Block in a background thread to wait for the stepOver to complete
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
          try {
            LOGGER.warn("Waiting for step over to complete...");
            latch.await(); // Wait until latch.countDown() is called
            LOGGER.warn("Step over finished, continuing execution.");
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Thread was interrupted while waiting for step over to complete", e);
          }
        });

        stepedOver = true;
      } else {
        LOGGER.warn("Trying to resume");
        ApplicationManager.getApplication().invokeLater(debugSession::resume);
        LOGGER.warn("Resuming");
        stepedOver = false;
      }*/
    }
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

  private String saveStateToFile(String state) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {
      writer.write(state);
      writer.write("\n====================\n"); // Separate different breakpoints
      LOGGER.warn("Successfully saved collected state to file");
      return "Successfully saved collected state to file: " + outputFile.getAbsolutePath();
    } catch (IOException e) {
      return "Error saving collected state to file";
    }
  }
}
