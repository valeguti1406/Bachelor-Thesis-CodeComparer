package com.thesis.codecomparer.Comparators;

import com.google.gson.Gson;
import com.thesis.codecomparer.dataModels.BreakpointState;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileComparator {

    private static final String SEPARATOR = "====================";

    public static String compareFiles(String filePath1, String filePath2) {
        try {
            // Parse the files into BreakpointState objects
            List<BreakpointState> file1States = parseFile(filePath1);
            List<BreakpointState> file2States = parseFile(filePath2);

            // Compare the files
            List<String> differences = StateComparator.compareStates(file1States, file2States);

            // Generate a comparison report
            if (differences.isEmpty()) {
                return "The files are identical.";
            } else {
                StringBuilder report = new StringBuilder("Differences found:\n");
                for (String diff : differences) {
                    report.append(diff).append("\n");
                }
                return report.toString();
            }
        } catch (IOException e) {
            return "Error reading files: " + e.getMessage();
        }
    }
    public static List<BreakpointState> parseFile(String filePath) throws IOException {
        List<BreakpointState> states = new ArrayList<>();
        Gson gson = new Gson();
        StringBuilder jsonBlock = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().equals(SEPARATOR)) {
                    if (jsonBlock.length() > 0) {
                        // Parse the JSON block into a BreakpointState object
                        BreakpointState state = gson.fromJson(jsonBlock.toString(), BreakpointState.class);
                        states.add(state);
                        jsonBlock.setLength(0); // Clear the buffer for the next block
                    }
                } else {
                    jsonBlock.append(line).append("\n");
                }
            }
            // Parse the last block (if exists)
            if (jsonBlock.length() > 0) {
                BreakpointState state = gson.fromJson(jsonBlock.toString(), BreakpointState.class);
                states.add(state);
            }
        }

        return states;
    }

    public static String generateGroupedReport(List<BreakpointState> file1States, List<BreakpointState> file2States) {
        StringBuilder report = new StringBuilder("Differences found:\n\n");

        int maxBlocks = Math.max(file1States.size(), file2States.size());
        for (int i = 0; i < maxBlocks; i++) {
            report.append("Block ").append(i + 1).append(":\n");

            if (i >= file1States.size()) {
                report.append("  - Extra block in File 2:\n    ").append(file2States.get(i)).append("\n");
            } else if (i >= file2States.size()) {
                report.append("  - Extra block in File 1:\n    ").append(file1States.get(i)).append("\n");
            } else {
                List<String> differences = StateComparator.compareBreakpointStates(file1States.get(i), file2States.get(i), "");
                if (differences.isEmpty()) {
                    report.append("  - No differences\n");
                } else {
                    for (String diff : differences) {
                        report.append("  - ").append(diff).append("\n");
                    }
                }
            }

            report.append("\n");
        }

        return report.toString();
    }
}
