package com.thesis.codecomparer.variableExtractor;

import com.sun.jdi.*;
import com.thesis.codecomparer.ui.CodeComparerUI;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class for invoking methods on `ObjectReference` instances during debugging.
 *
 * Provides a mechanism to:
 * - Dynamically invoke methods on objects obtained from the Java Debug Interface (JDI).
 * - Handle method selection, argument validation, and exception handling.
 * - Update the user interface in case of invocation errors.
 */
public class ValueUtil {

    /**
     * Invokes a method on a given `ObjectReference` with the specified arguments.
     *
     * This method attempts to match and invoke a method by name, considering overloaded
     * variants. If no matching method is found or invocation fails, errors are logged
     * and displayed to the user interface.
     *
     * @param object the `ObjectReference` representing the target object.
     * @param methodName the name of the method to invoke.
     * @param thread the `ThreadReference` representing the thread for execution.
     * @param args the arguments to pass to the method during invocation.
     * @return the result of the method invocation as a `Value`, or `null` if invocation fails.
     * @throws NoSuchMethodError if no method with the specified name is found.
     */
    public static Value invokeMethod(
            ObjectReference object, String methodName, ThreadReference thread, Value... args) {
        // Retrieve all methods with the specified name
        List<Method> methods = object.referenceType().methodsByName(methodName);
        if (methods.isEmpty()) {
            throw new NoSuchMethodError(methodName); // No method found with the given name
        }

        // Access the user interface to display error messages if needed
        CodeComparerUI codeComparerUI = CodeComparerUI.getInstance();

        try {
            boolean invokeSuccessful = false; // Track whether any method invocation succeeded

            // Iterate through available methods and attempt invocation
            for (Method m : methods) {
                List<Type> argType = m.argumentTypes();

                // Check if argument count matches (support for varargs)
                if (argType.size() == args.length - 1 || argType.size() == args.length) {
                    try {
                        // Attempt to invoke the method
                        Value returnValue = object.invokeMethod(thread, m, Arrays.asList(args), 0);
                        invokeSuccessful = true; // Mark invocation as successful
                        return returnValue; // Return the result
                    } catch (IllegalArgumentException ignored) {
                        // Ignore and proceed to try other methods
                    }
                }
            }

            // Update UI if no method matched for invocation
            if (!invokeSuccessful) {
                codeComparerUI.updateErrorDisplay("No method matched to invoke it");
            }
        } catch (Exception e) {
            // Handle unexpected exceptions during method invocation
            codeComparerUI.updateErrorDisplay("Exception invoking method: " + e.getMessage());
        }

        return null; // Return null if invocation fails
    }
}

