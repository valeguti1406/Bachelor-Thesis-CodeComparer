package com.thesis.codecomparer.ui;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.thesis.codecomparer.comparators.FileComparator;
import com.thesis.codecomparer.dataModels.BreakpointState;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.*;

/**
 * Manages the UI for the CodeComparer functionality within the IntelliJ Debugger tab. This class
 * provides the following: - A file selection interface for choosing files to compare. - An
 * integrated area for displaying comparison reports directly in the debugging tab.
 *
 * <p>Workflow: 1. Allows users to select two files for comparison via a file chooser dialog. 2.
 * Parses the selected files and compares their contents using FileComparator. 3. Displays the
 * results in a scrollable text area within the tab.
 */
public class CodeComparerUI {
  private static CodeComparerUI instance; // Singleton instance of the UI
  private final JPanel mainPanel; // Main container for the debugging tab
  private JLabel errorLabel; // Label to display errors

  private JPanel errorPanel; // Panel to display errors
  private JTextField filePathField; // Text field to display the file path
  private final JTextArea reportArea; // Text area to display the report

  /**
   * Private constructor to initialize the UI components. Use the getInstance() method to access the
   * singleton instance.
   */
  private CodeComparerUI() {
    mainPanel = new JPanel(new GridBagLayout()); // Use GridBagLayout for flexible sizing
    reportArea = createReportArea();

    // Add components to the main panel with appropriate layout constraints
    addStatusPanels(); // Add Status Panels (Error and File Path)
    addFileSelectionPanel(); // Add File Selection Panel second
    addReportArea(); // Add Report Panel last
  }

  /** Adds the status panels (file path and error panels) to the main panel. */
  private void addStatusPanels() {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = JBUI.insets(5);

    // Add File Path Panel (Top)
    JPanel filePathPanel = createFilePathPanel();
    gbc.gridy = 0; // Topmost position
    mainPanel.add(filePathPanel, gbc);

    // Add Error Panel (Bottom)
    errorPanel = createErrorPanel(); // Use class-level errorPanel
    gbc.gridy = 1; // Bottom position
    mainPanel.add(errorPanel, gbc);
  }

  /**
   * Creates the file path panel to display and copy file paths.
   *
   * @return A JPanel containing the file path field and a copy button with a title.
   */
  private JPanel createFilePathPanel() {
    JPanel filePathPanel = new JPanel(new BorderLayout());
    filePathPanel.setBorder(BorderFactory.createTitledBorder("File Path"));

    // File Path Text Field
    filePathField = new JTextField();
    filePathField.setEditable(false); // Make read-only
    filePathPanel.add(filePathField, BorderLayout.CENTER);

    // Copy Button
    JButton copyButton = new JButton("Copy");
    copyButton.addActionListener(e -> copyFilePathToClipboard());
    filePathPanel.add(copyButton, BorderLayout.EAST);

    return filePathPanel;
  }

  /**
   * Updates the file path display with a new file path.
   *
   * @param filePath The file path to display.
   */
  public void updateFilePathDisplay(String filePath) {
    filePathField.setText(filePath);
  }

  /**
   * Creates the error panel to display error messages or a "No Errors" message.
   *
   * @return A JPanel containing the error label.
   */
  private JPanel createErrorPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createTitledBorder("Error Status"));

    errorLabel = new JLabel("Successful!  No errors detected :)"); // Default message
    errorLabel.setForeground(JBColor.GREEN); // Default to green for no errors
    errorLabel.setHorizontalAlignment(SwingConstants.LEFT);
    panel.add(errorLabel, BorderLayout.CENTER);

    return panel;
  }

  /**
   * Updates the error message display. If there are errors, display the error message in red.
   *
   * @param message The error message to display (null or empty indicates no errors).
   */
  public void updateErrorDisplay(String message) {
    if (message != null && !message.trim().isEmpty()) {
      errorLabel.setText(message);
      errorLabel.setForeground(JBColor.RED); // Red for errors
    }

    // Refresh the main panel to reflect the changes
    mainPanel.revalidate();
    mainPanel.repaint();
  }

  /** Adds the file selection panel to the main panel with appropriate constraints. */
  private void addFileSelectionPanel() {
    JPanel fileSelectionPanel = createFileSelectionPanel();
    GridBagConstraints gbc = new GridBagConstraints();

    // File Selection Panel (second section)
    gbc.gridx = 0;
    gbc.gridy = 2; // Below file path panel
    gbc.weightx = 1;
    gbc.weighty = 0.1; // Smaller weight for height
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = JBUI.insets(5);
    mainPanel.add(fileSelectionPanel, gbc);
  }

  /** Adds the report area to the main panel with appropriate constraints. */
  private void addReportArea() {
    GridBagConstraints gbc = new GridBagConstraints();

    // Report Area (last section, takes most of the space)
    gbc.gridx = 0;
    gbc.gridy = 3; // Below file selection panel
    gbc.weightx = 1;
    gbc.weighty = 0.8; // Larger weight for height
    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = JBUI.insets(5);
    mainPanel.add(new JBScrollPane(reportArea), gbc);
  }

  /**
   * Provides the singleton instance of DebuggerCodeComparerUI. This prevents accidental creation of
   * multiple UI instances
   *
   * @return The single DebuggerCodeComparerUI instance.
   */
  public static CodeComparerUI getInstance() {
    if (instance == null) {
      instance = new CodeComparerUI();
    }
    return instance;
  }

  /**
   * Creates the file selection panel with a button for selecting files and generating the report.
   *
   * @return A JPanel containing the file selection button.
   */
  private JPanel createFileSelectionPanel() {
    JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    filePanel.setBorder(BorderFactory.createTitledBorder("File Selection"));

    // Button for selecting files
    JButton selectFilesButton = new JButton("Select Files & Compare");
    selectFilesButton.addActionListener(e -> showFileSelectionDialog());

    // Add the button to the panel
    filePanel.add(selectFilesButton);

    return filePanel;
  }

  /**
   * Creates the text area for displaying the comparison report.
   *
   * @return A JTextArea wrapped in a titled border for displaying the comparison report.
   */
  private JTextArea createReportArea() {
    JTextArea textArea = new JTextArea("Please select files to compare.");
    textArea.setEditable(false);
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea.setBorder(BorderFactory.createTitledBorder("Comparison Report"));
    return textArea;
  }

  /** Handles the file selection process, performs the comparison, and displays the results. */
  public void showFileSelectionDialog() {
    String file1Path = selectFile("Select First File to Compare");
    if (file1Path == null) return;

    String file2Path = selectFile("Select Second File to Compare");
    if (file2Path == null) return;

    String result;
    try {
      // Parse and compare the files
      List<BreakpointState> file1States = FileComparator.parseFile(file1Path);
      List<BreakpointState> file2States = FileComparator.parseFile(file2Path);

      // Extract file names from file paths
      String file1Name = new File(file1Path).getName();
      String file2Name = new File(file2Path).getName();

      // Generate the report
      result = FileComparator.generateGroupedReport(file1States, file2States, file1Name, file2Name);
    } catch (IOException e) {
      result = "Error while parsing file: " + e.getMessage();
    }

    // Display the result in the report area
    reportArea.setText(result);
  }

  /**
   * Opens a file chooser dialog to let the user select a file.
   *
   * @param title The title of the file chooser dialog (e.g., "Select First File to Compare").
   * @return The absolute path of the selected file, or `null` if no file is selected.
   */
  private String selectFile(String title) {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle(title);
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setFileFilter(
        new javax.swing.filechooser.FileNameExtensionFilter("Text Files (*.txt)", "txt"));

    int returnValue = fileChooser.showOpenDialog(null);
    if (returnValue == JFileChooser.APPROVE_OPTION) {
      File selectedFile = fileChooser.getSelectedFile();
      return selectedFile.getAbsolutePath();
    }
    return null;
  }

  /** Copies the file path to the clipboard. */
  private void copyFilePathToClipboard() {
    String filePath = filePathField.getText();
    Toolkit.getDefaultToolkit()
        .getSystemClipboard()
        .setContents(new StringSelection(filePath), null);
  }

  /**
   * Returns the main panel containing the UI components.
   *
   * @return The JPanel containing the debugging tab's UI.
   */
  public JPanel getMainPanel() {
    return mainPanel;
  }

  /**
   * Resets the UI components to their initial state by removing all existing elements and re-adding
   * them. This ensures that the UI is refreshed when a new debugging session starts, preventing
   * outdated messages from persisting.
   */
  public void resetUI() {
    // Remove all components from the panel
    mainPanel.removeAll();

    // Re-add components to restore the initial UI state
    addStatusPanels();
    addFileSelectionPanel();
    addReportArea();

    // Refresh UI
    mainPanel.revalidate();
    mainPanel.repaint();
  }
}
