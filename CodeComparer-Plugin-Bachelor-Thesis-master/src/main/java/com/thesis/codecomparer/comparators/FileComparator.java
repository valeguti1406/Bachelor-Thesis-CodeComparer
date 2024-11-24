package com.thesis.codecomparer.comparators;

import com.google.gson.Gson;
import com.thesis.codecomparer.dataModels.BreakpointState;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileComparator {

    private static final String SEPARATOR = "====================";

    /**
     * Parses a file containing JSON blocks separated by a predefined separator and converts them into a list of BreakpointState objects.
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
                        //parse the JSON Breakpoint and add to the list
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
     * Generates a grouped report highlighting differences between two lists of BreakpointState objects.
     *
     * @param file1States A list of BreakpointState objects from the first file.
     * @param file2States A list of BreakpointState objects from the second file.
     * @return A formatted string report detailing differences between the two lists.
     */
    public static String generateGroupedReport(List<BreakpointState> file1States, List<BreakpointState> file2States) {
        StringBuilder report = new StringBuilder("Differences found:\n\n");

        int maxBlocks = Math.max(file1States.size(), file2States.size()); // Determine the larger list
        for (int i = 0; i < maxBlocks; i++) {
            report.append("Breakpoint ").append(i + 1).append(":\n"); // Add a name for this Breakpoint to the report 

            // If file 2 has extra Breakpoints
            if (i >= file1States.size()) {
                report.append("  - Extra Breakpoint in File 2:\n    ").append(file2States.get(i)).append("\n");
            }
            // If file 1 has extra Breakpoints
            else if (i >= file2States.size()) {
                report.append("  - Extra Breakpoint in File 1:\n    ").append(file1States.get(i)).append("\n");
            }
            // Compare corresponding Breakpoint from both files
            else {
                List<String> differences = StateComparator.compareBreakpointStates(file1States.get(i), file2States.get(i), "");
                if (differences.isEmpty()) {
                    report.append("  - No differences\n"); // No differences in this Breakpoint
                } else {
                    // Add all differences for this block
                    for (String diff : differences) {
                        report.append("  - ").append(diff).append("\n");
                    }
                }
            }

            report.append("\n"); // Add spacing between Breakpoint
        }

        return report.toString(); // Return the final report
    }

}
