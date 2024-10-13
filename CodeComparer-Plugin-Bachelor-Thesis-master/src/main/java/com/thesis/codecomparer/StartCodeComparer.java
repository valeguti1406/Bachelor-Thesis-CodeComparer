package com.thesis.codecomparer;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

/**
 * Class called when a user clicks on the CodeComparer action.
 */
public class StartCodeComparer extends AnAction{

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        event.getPresentation().setEnabledAndVisible(project != null); // show action just if a project is open
        //TODO: just if class is runnable
    }

    /**
     * Callback for button clicked. Initiates CodeComparer.
     * @param event CodeComparer button event click
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        RunnerAndConfigurationSettings config =
                RunManagerImpl.getInstanceEx(event.getProject())
                        .getSelectedConfiguration();
        if (config != null) {
            //TODO: debug the class and in each breakpoint save variable info and line
            //TODO: display variable info
            Messages.showMessageDialog(
                    config.toString(),
                    "RunnerAndConfigurationSettings:",
                    Messages.getInformationIcon());
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT; // action should run in a background thread
    }

}
