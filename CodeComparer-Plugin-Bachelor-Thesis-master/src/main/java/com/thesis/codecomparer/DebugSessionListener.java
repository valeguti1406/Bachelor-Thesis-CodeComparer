package com.thesis.codecomparer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.debugger.engine.JavaStackFrame;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.debugger.settings.DebuggerSettings;
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
import com.thesis.codecomparer.dataModels.BreakpointState;
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

  private final String outputDirectoryPath = "CodeComparer-Plugin/output";
  private final String outputFileName = "collected_states.txt";
  private File outputFile;

  private StringBuilder infoToDisplay = new StringBuilder();
  private boolean isStepping = false; // General stepping state
  private boolean isSteppingInto = false; // Track if we are in a step-into operation
  private boolean isSteppingOut = false; // Track if we are in a step-out operation

  private BreakpointState breakpointState;

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
                activateReturnValueSetting();
              }
            });
  }

  @Override
  public void sessionPaused() {
    LOGGER.warn("Debugger paused");
    JavaStackFrame javaStackFrame = (JavaStackFrame) debugSession.getCurrentStackFrame();

    BreakpointStateCollector breakpointStateCollector = getBreakpointStateCollector();
    if (breakpointStateCollector == null) return;
    // empty the StringBuilder
    infoToDisplay.setLength(0);

    if (!isStepping) {
      // First pause: collect current method and step into the called method
      LOGGER.warn("Collecting current method info and stepping into called method");
      breakpointState = new BreakpointState();
      collectAndStepInto(breakpointStateCollector, javaStackFrame);
    } else if (isSteppingInto) {
      // After step into: collect called method info and step out
      LOGGER.warn("Step into completed, collecting called method info and stepping out");
      collectAndStepOut(breakpointStateCollector, javaStackFrame);
    } else if (isSteppingOut) {
      // After step out: collect return value and resume
      LOGGER.warn("Step out completed, collecting return value and resuming");
      collectReturnValueAndResume(breakpointStateCollector, javaStackFrame);
    }
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

  private void collectAndStepInto(
      BreakpointStateCollector breakpointStateCollector, JavaStackFrame javaStackFrame) {

    appendFileNameAndLine();

    // Collect current method details
    breakpointState.setCurrentMethodState(breakpointStateCollector.getMethodState(javaStackFrame));

    LOGGER.warn("Initiating step into...");
    // Mark that stepping into is starting
    isStepping = true;
    isSteppingInto = true;

    // Perform the step into
    ApplicationManager.getApplication().invokeLater(debugSession::stepInto);
  }

  private void collectAndStepOut(
          BreakpointStateCollector breakpointStateCollector, JavaStackFrame javaStackFrame) {

    // Collect details of the called method (after stepping into)
    breakpointState.setBreakpointMethodCallState(breakpointStateCollector.getMethodState(javaStackFrame));

    LOGGER.warn("Initiating step out...");
    // Mark that the stepping into is complete and step out is starting
    isSteppingInto = false;
    isSteppingOut = true;

    // Perform the step out
    ApplicationManager.getApplication().invokeLater(debugSession::stepOut);
  }

  private void collectReturnValueAndResume(
      BreakpointStateCollector breakpointStateCollector, JavaStackFrame javaStackFrame) {

    // Collect the return value after stepping out
    breakpointState.setBreakpointReturnValue(
        breakpointStateCollector.getReturnValue(javaStackFrame));

    // Save the complete BreakpointState to file
    saveStateToFile(breakpointState);

    LOGGER.warn("Resuming program...");
    // Mark that the stepping is complete
    isStepping = false;
    isSteppingOut = false;
    ApplicationManager.getApplication().invokeLater(debugSession::resume);
  }

  private void appendFileNameAndLine() {
    if (debugSession.getCurrentPosition() != null) {
      String fileName = debugSession.getCurrentPosition().getFile().getNameWithoutExtension();
      int line = debugSession.getCurrentPosition().getLine() + 1;
      breakpointState.setFileName(fileName);
      breakpointState.setBreakpointInLine(line);
    }
  }

  private StackFrameProxyImpl getStackFrameProxy() {
    JavaStackFrame currentStackFrame = (JavaStackFrame) debugSession.getCurrentStackFrame();
    if (currentStackFrame == null) {
      LOGGER.warn("Current stack frame could not be found!");
      displayStateInPanel("Current stack frame could not be found!");
      return null;
    }
    else {
      return currentStackFrame.getStackFrameProxy();
    }
  }

  private BreakpointStateCollector getBreakpointStateCollector(){
    StackFrameProxyImpl stackFrame = getStackFrameProxy();
    if (stackFrame == null) {
      LOGGER.warn("No stack frame available!");
      displayStateInPanel("No stack frame available!");
      return null;
    }
    return new BreakpointStateCollector(stackFrame);
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

  /** Activate the "Show Method Return Values" option in the Debugger Settings */
  private void activateReturnValueSetting() {
    // Ensure the setting is enabled when the debugger session starts
    DebuggerSettings debuggerSettings =
        ApplicationManager.getApplication().getService(DebuggerSettings.class);
    if (debuggerSettings != null) {
      debuggerSettings.WATCH_RETURN_VALUES = true; // Enable the setting
      LOGGER.warn("Return Values Watch turned on");
    }
  }

  private void saveStateToFile(BreakpointState breakpointState) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      String json = gson.toJson(breakpointState);

      writer.write(json);
      writer.write("\n====================\n"); // Separate different breakpoints
      LOGGER.warn("Successfully saved collected state to file");
      displayStateInPanel(
          "Successfully saved collected state to file: " + outputFile.getAbsolutePath());
    } catch (IOException e) {
      displayStateInPanel("Error saving collected state to file");
    }
  }
}
