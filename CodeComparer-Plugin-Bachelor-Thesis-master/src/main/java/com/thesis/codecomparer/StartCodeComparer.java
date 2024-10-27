package com.thesis.codecomparer;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

/** Class called when a user clicks on the CodeComparer action. */
public class StartCodeComparer extends AnAction {

  /**
   * Callback for button clicked. Initiates Debugging for the most recent executed program.
   *
   * @param event CodeComparer action event click
   */
  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    // get the runner/debug configuration from the project
    RunnerAndConfigurationSettings config =
        RunManagerImpl.getInstanceEx(event.getProject()).getSelectedConfiguration();
    if (config != null) {
      Messages.showMessageDialog(
          config.toString(), "RunnerAndConfigurationSettings:", Messages.getInformationIcon());
    }
  }
}
