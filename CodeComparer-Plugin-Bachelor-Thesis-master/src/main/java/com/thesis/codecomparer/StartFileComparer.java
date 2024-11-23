package com.thesis.codecomparer;


import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.components.JBScrollPane;
import com.thesis.codecomparer.Comparators.FileComparator;
import com.thesis.codecomparer.dataModels.BreakpointState;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/** Class called when a user clicks on the CodeComparer action. */
public class StartFileComparer extends AnAction {

  /**
   * Callback for button clicked. Initiates Debugging for the most recent executed program.
   *
   * @param event CodeComparer action event click
   */
  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    // Create file chooser dialogs
    String file1 = selectFile("Select First File to Compare");
    if (file1 == null) return;

    String file2 = selectFile("Select Second File to Compare");
    if (file2 == null) return;

    // Call the comparison method
    //String result = FileComparator.compareFiles(file1, file2);

    String result;
    try {
      java.util.List<BreakpointState> file1States = FileComparator.parseFile(file1);
      java.util.List<BreakpointState> file2States = FileComparator.parseFile(file2);

      result = FileComparator.generateGroupedReport(file1States, file2States);
    } catch (IOException e) {
      result = "Error while parsing file " + e.getMessage();
    }

    // Show the result in a popup
    showComparisonResult(result);
  }

  private String selectFile(String title) {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle(title);
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    int returnValue = fileChooser.showOpenDialog(null);
    if (returnValue == JFileChooser.APPROVE_OPTION) {
      File selectedFile = fileChooser.getSelectedFile();
      return selectedFile.getAbsolutePath();
    }
    return null; // No file selected
  }

  public static void showComparisonResult(String result) {
    JDialog dialog = new JDialog();
    dialog.setTitle("Comparison Result");

    JTextArea textArea = new JTextArea(result);
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea.setEditable(false);

    JScrollPane scrollPane = new JBScrollPane(textArea);
    scrollPane.setPreferredSize(new Dimension(1000, 400));

    dialog.add(scrollPane);
    dialog.pack();
    dialog.setLocationRelativeTo(null); // Center the dialog
    dialog.setVisible(true);
  }
}
