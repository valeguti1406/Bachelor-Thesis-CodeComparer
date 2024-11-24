package com.thesis.codecomparer.ui;


import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;


/**
 * Action triggered when the user clicks on the "CodeComparer" symbol in the added Tab of the IntelliJ Debugger.
 * This action serves as an entry point to interact with the CodeComparer UI.
 *
 * Workflow:
 * 1. Delegates file selection, comparison, and report generation to the DebuggerCodeComparerUI.
 * 2. Ensures user actions in the "CodeComparer" tab are routed to the appropriate UI functionality.
 */
public class StartFileComparer extends AnAction {

  /**
   * Entry point for the CodeComparer action. Called when the button is clicked.
   *
   * Workflow:
   * 1. Invokes the file selection and report generation logic in the DebuggerCodeComparerUI.
   * 2. Allows the user to select two files and view the differences directly in the CodeComparer tab.
   *
   * @param event The IntelliJ action event triggered by the button click.
   */
  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    // Delegate file selection and report generation to the DebuggerCodeComparerUI
    CodeComparerUI ui = CodeComparerUI.getInstance();
    ui.showFileSelectionDialog();
  }

}
