**Project Overview**

CodeComparer is an IntelliJ plugin designed to help developers detect hidden functional changes introduced by third-party library updates. 
Unlike traditional tools like Git diffs or API diff tools, which focus on static code changes, CodeComparer captures runtime behavior during debugging sessions.

The plugin integrates with the IntelliJ Debugger and works passively: developers simply set breakpoints in code they want to monitor—either because it may be affected by a library update or because they want to observe its runtime behavior—and CodeComparer automatically collects detailed runtime data whenever a breakpoint is hit.
This data is exported as JSON objects and written to a text file, enabling comparison across runs (e.g., before and after updating a library). This helps uncover behavioral differences that static analysis and tests might miss.


**Features**

- **Automatic Runtime Monitoring at Breakpoints**
  Once installed, CodeComparer works automatically during any Java debug session, no configuration required.
  Whenever a breakpoint is hit, the plugin silently records relevant runtime data and immediately resumes execution. This means the developer doesn’t need to manually click Resume, CodeComparer keeps the debugging flow uninterrupted.

- **Dedicated IntelliJ Debug Tab**  
  When a debug session starts, CodeComparer appears as a new tab in the IntelliJ Debug tool window, alongside standard views like **Threads & Variables** and **Console**.  
  This tab provides:
  - The file path of the captured output (with a Copy button for convenience)
  - Live feedback on capture status (success or error)
  - A built-in interface for comparing files from different runs

- **Captures detailed runtime information**  
  For each breakpoint hit, the following data is extracted:
  - File name and line number
  - Current method state, including:
    - Method name
    - Return type
    - Input parameters (each with a name and a serialized representation).
  - If a method is invoked on the breakpoint line:
    - Method name
    - Return type
    - Input parameters (each with a name and a serialized representation)
    - Return value
  - Exception details, if one is thrown (type, message, and full stack trace)

- **Exported as structured JSON**  
  Each individual breakpoint hit is stored as a standalone JSON object. All captured JSONs from a session are saved line-by-line in a single `.txt` file, making it easy to compare data across runs.

- **Cross-version comparison support**  
  By comparing the generated output files before and after a library update, developers can identify changes in runtime behavior, even when tests still pass and compilation succeeds.

**Running the Plugin Locally**

To test CodeComparer inside IntelliJ, you can launch a sandboxed instance of the IDE using the `runIde` Gradle task.

1. **Run the plugin in a sandboxed IDE**  
   Execute the `runIde` Gradle task.  
   This will start a development instance of IntelliJ with the plugin installed and enabled.

2. **Set breakpoints and start debugging**  
   Open any project you want to test. Set breakpoints in code you want to monitor—either because it may be affected by a library update or because you want to observe its runtime behavior—and start a debugging session.

3. **View captured output in the CodeComparer tab**  
   Every time a breakpoint is hit, CodeComparer captures a snapshot of the runtime state.  
   - Each snapshot is stored as a JSON object.  
   - All captured snapshots from the session are saved to a plain text file.  
   - The file path is shown in the **CodeComparer tab** under the **File Path** section. You can easily copy it to your clipboard using the **Copy** button.

4. **Check for capture status**  
   CodeComparer provides immediate visual feedback:
   - If the capture is successful: a green message appears saying **"Successful! No errors detected :)"**  
   - If an error occurs (e.g., missing debug info or inaccessible arguments): a red error message is shown in the **Error Status** field

**Comparing Captured Files**

After collecting runtime data from two different executions—typically before and after a library update—you can compare them directly within the plugin.

To run a comparison:

1. Click **Select Files & Compare** in the CodeComparer tab
2. Choose the first `.txt` file (e.g., from the pre-update run)
3. Choose the second `.txt` file (e.g., from the post-update run)

The plugin will automatically generate a **Comparison Report**, displayed inside the CodeComparer tab. The report includes:

- **Compared Files:** Lists the paths of the two files being compared  
- **Differences Found:** For each breakpoint, highlights all differing fields in the format:  
  ```
  <value in file 1> != <value in file 2>
  ```
- **Summary:**  
  - Total breakpoints compared  
  - Number of breakpoints with differences  
  - Number of breakpoints with no differences  
  - Notes if one file has more breakpoints or they are out of order

This allows developers to quickly detect subtle behavioral differences—such as changed return values or new exceptions.



