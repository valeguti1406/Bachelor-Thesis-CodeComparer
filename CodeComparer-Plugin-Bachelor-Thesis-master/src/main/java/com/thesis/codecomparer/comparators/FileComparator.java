package com.thesis.codecomparer.comparators;

import com.google.gson.Gson;
import com.thesis.codecomparer.dataModels.BreakpointState;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileComparator {

  private static final String SEPARATOR = "====================";

  /**
   * Parses a file containing JSON blocks separated by a predefined separator and converts them into
   * a list of BreakpointState objects.
   *
   * @param filePath The path to the file to be parsed.
   * @return A list of BreakpointState objects parsed from the file.
   * @throws IOException If an error occurs while reading the file.
   */
  public static List<BreakpointState> parseFile(String filePath) throws IOException {
    List<BreakpointState> states = new ArrayList<>(); // To store parsed BreakpointState objects
    Gson gson = new Gson(); // Gson instance for JSON parsing
    StringBuilder jsonBreakpoint = new StringBuilder(); // Temporary storage for JSON Breakpoint

    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
      String line;
      while ((line = reader.readLine()) != null) {
        // Check if the current line is the Breakpoint separator
        if (line.trim().equals(SEPARATOR)) {
          if (!jsonBreakpoint.isEmpty()) {
            // parse the JSON Breakpoint and add to the list
            BreakpointState state = gson.fromJson(jsonBreakpoint.toString(), BreakpointState.class);
            states.add(state);
            jsonBreakpoint.setLength(0); // Clear the buffer for the next Breakpoint
          }
        } else {
          // Append the line to the current JSON Breakpoint
          jsonBreakpoint.append(line).append("\n");
        }
      }
    }

    return states; // Return the list of parsed BreakpointState objects
  }

  /**
   * Generates a grouped report highlighting differences between two lists of BreakpointState
   * objects.
   *
   * @param file1States A list of BreakpointState objects from the first file.
   * @param file2States A list of BreakpointState objects from the second file.
   * @param file1Name Name of the first file being compared.
   * @param file2Name Name of the second file being compared.
   * @return A formatted string report detailing differences between the two lists.
   */
  public static String generateGroupedReport(
      List<BreakpointState> file1States,
      List<BreakpointState> file2States,
      String file1Name,
      String file2Name) {
    StringBuilder report = new StringBuilder();

    // Add header for file comparison
    addComparisonHeader(report, file1Name, file2Name);

    // Initialize summary trackers
    List<String> breakpointsWithDiffs = new ArrayList<>();
    List<String> breakpointsWithoutDiffs = new ArrayList<>();
    int breakpointsWithDifferences = 0;
    int breakpointsWithoutDifferences = 0;

    int totalBreakpoints = Math.max(file1States.size(), file2States.size());

    for (int i = 0; i < totalBreakpoints; i++) {
      String location = getBreakpointLocation(i, file1States, file2States);

      // Collect differences for the current breakpoint
      List<String> differences = getBreakpointDifferences(i, file1States, file2States);

      if (differences.isEmpty()) {
        // No differences for this breakpoint
        breakpointsWithoutDiffs.add("Breakpoint " + (i + 1) + location);
        breakpointsWithoutDifferences++;
      } else {
        // Append differences to the report
        appendBreakpointWithDifferences(report, i, location, differences);
        breakpointsWithDiffs.add("Breakpoint " + (i + 1) + location);
        breakpointsWithDifferences++;
      }
    }

    // Add summary section
    appendSummary(
        report,
        totalBreakpoints,
        breakpointsWithDifferences,
        breakpointsWithoutDifferences,
        breakpointsWithDiffs,
        breakpointsWithoutDiffs);

    return report.toString();
  }

  /**
   * Adds a header section for file comparison to the report.
   *
   * @param report The StringBuilder to append the header to.
   * @param file1Name Name of the first file being compared.
   * @param file2Name Name of the second file being compared.
   */
  private static void addComparisonHeader(
      StringBuilder report, String file1Name, String file2Name) {
    report.append("=== Comparing Files ===\n");
    report.append("- File 1: ").append(file1Name).append("\n");
    report.append("- File 2: ").append(file2Name).append("\n\n");
    report.append("=== Differences Found ===\n\n");
  }

  /**
   * Retrieves the file name and line number information for a breakpoint.
   *
   * @param index The index of the breakpoint.
   * @param file1States List of breakpoints from the first file.
   * @param file2States List of breakpoints from the second file.
   * @return A string describing the breakpoint's file and line number.
   */
  private static String getBreakpointLocation(
      int index, List<BreakpointState> file1States, List<BreakpointState> file2States) {
    if (index < file1States.size()) {
      BreakpointState state1 = file1States.get(index);
      return " (File: " + state1.getFileName() + ", Line: " + state1.getBreakpointInLine() + ")";
    } else if (index < file2States.size()) {
      BreakpointState state2 = file2States.get(index);
      return " (File: " + state2.getFileName() + ", Line: " + state2.getBreakpointInLine() + ")";
    }
    return "";
  }

  /**
   * Compares the breakpoints at the specified index and identifies differences.
   *
   * @param index The index of the breakpoint.
   * @param file1States List of breakpoints from the first file.
   * @param file2States List of breakpoints from the second file.
   * @return A list of strings describing differences, or an empty list if no differences exist.
   */
  private static List<String> getBreakpointDifferences(
      int index, List<BreakpointState> file1States, List<BreakpointState> file2States) {
    if (index < file1States.size() && index < file2States.size()) {
      return StateComparator.compareBreakpointStates(
          file1States.get(index), file2States.get(index));
    }
    return new ArrayList<>();
  }

  /**
   * Appends details of a breakpoint with differences to the report.
   *
   * @param report The StringBuilder to append to.
   * @param index The index of the breakpoint.
   * @param location The file and line number location of the breakpoint.
   * @param differences A list of strings describing the differences.
   */
  private static void appendBreakpointWithDifferences(
      StringBuilder report, int index, String location, List<String> differences) {
    report.append("=== Breakpoint ").append(index + 1).append(location).append(" ===\n");
    for (String diff : differences) {
      if (diff.startsWith("  Current Method ->")) {
        report.append("  Current Method:\n");
      } else if (diff.startsWith("  Invoked Method ->")) {
        report.append("  Invoked Method:\n");
      } else if (diff.startsWith("  - ")) {
        report.append("    ").append(diff.replace("  - ", "- ")).append("\n");
      } else {
        report.append("    ").append(diff).append("\n");
      }
    }
    report.append("\n");
  }

  /**
   * Appends a summary of the comparison results to the report.
   *
   * @param report The StringBuilder to append the summary to.
   * @param totalBreakpoints The total number of breakpoints compared.
   * @param breakpointsWithDifferences The number of breakpoints with differences.
   * @param breakpointsWithoutDifferences The number of breakpoints without differences.
   * @param breakpointsWithDiffs A list of breakpoints with differences.
   * @param breakpointsWithoutDiffs A list of breakpoints without differences.
   */
  private static void appendSummary(
      StringBuilder report,
      int totalBreakpoints,
      int breakpointsWithDifferences,
      int breakpointsWithoutDifferences,
      List<String> breakpointsWithDiffs,
      List<String> breakpointsWithoutDiffs) {
    report.append("=== Summary ===\n");
    report.append("- Total Breakpoints: ").append(totalBreakpoints).append("\n\n");
    report
        .append("- Breakpoints with Differences: ")
        .append(breakpointsWithDifferences)
        .append("\n");
    for (String bp : breakpointsWithDiffs) {
      report.append("  ").append(bp).append("\n");
    }
    report.append("\n");
    report
        .append("- Breakpoints without Differences: ")
        .append(breakpointsWithoutDifferences)
        .append("\n");
    for (String bp : breakpointsWithoutDiffs) {
      report.append("  ").append(bp).append("\n");
    }
  }
}
