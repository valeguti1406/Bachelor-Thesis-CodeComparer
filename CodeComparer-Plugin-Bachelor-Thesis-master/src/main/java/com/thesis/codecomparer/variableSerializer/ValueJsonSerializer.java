package com.thesis.codecomparer.variableSerializer;

import static com.thesis.codecomparer.variableSerializer.ValueUtil.invokeMethod;

import com.sun.jdi.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for serializing `Value` objects from the Java Debug Interface (JDI) to JSON format.
 *
 * <p>This class provides methods to: - Serialize primitives, strings, arrays, and objects to JSON.
 * - Handle nested objects and prevent infinite loops caused by circular references. - Customize
 * serialization behavior for common Java types like maps and collections.
 *
 * <p>Designed for use in debugging scenarios, where `Value` objects are obtained during a debugging
 * session and need to be converted into a JSON-compatible format for analysis or output.
 *
 * <p>This class is adapted from the Debug Variable Extractor project by chocovon, available at: <a
 * href="https://github.com/chocovon/debug-variable-extractor">Github</a>
 *
 * <p>Original license and copyright apply.
 */
public class ValueJsonSerializer {

  // Constant representing the qualified name of the `java.lang.Object` class.
  public static final String JAVA_LANG_OBJECT = "java.lang.Object";

  // The maximum allowed time (in milliseconds) for the JSON serialization process. Default value: 7
  // seconds.
  private static long timeLimit = 7000;

  // A timestamp indicating when the serialization process started.
  private static long timeStamp;

  /**
   * Converts a JDI `Value` to a JSON string.
   *
   * @param value The JDI `Value` to serialize.
   * @param thread The debugging thread reference, used to invoke methods on objects.
   * @param refPath A set to track visited object IDs and avoid circular references.
   * @return A JSON representation of the JDI `Value`.
   * @throws JsonSerializeException If serialization exceeds the time limit.
   */
  public static String toJson(Value value, ThreadReference thread, Set<Long> refPath) {
    // Record the start time of the serialization process
    timeStamp = System.currentTimeMillis();
    // Begin the recursive serialization process
    return toJsonInner(value, thread, refPath);
  }

  /**
   * Serializes a given JDI value to its JSON representation.
   *
   * <p>This method determines the type of the value (primitive, string, array, object, etc.) and
   * delegates the appropriate logic for each type.
   *
   * @param value the JDI value to serialize.
   * @param thread the current thread being used for method evaluation and serialization.
   * @param refPath a set containing the unique IDs of previously visited object references (for
   *     circular reference detection).
   * @return the JSON string representation of the value.
   */
  private static String toJsonInner(Value value, ThreadReference thread, Set<Long> refPath) {
    // Ensure serialization does not exceed the allowed time limit
    checkTimeLimit();

    // If value is null, return null as the JSON representation
    if (value == null) {
      return null;
    }

    // Detect circular references
    if (isCircularReference(value, refPath)) {
      return null;
    }

    // Handle primitive and string values
    String primitiveResult = handlePrimitiveAndStringValues(value);
    if (primitiveResult != null) return primitiveResult;

    // Handle arrays
    if (value instanceof ArrayReference) {
      return handleArrayValues(value, thread, refPath);
    }

    if (value instanceof ObjectReference) {
      Set<String> allInheritedTypes = getAllInheritedTypes(value);
      ObjectReference objectValue = (ObjectReference) value;

      // Handle simple objects like Integer, Boolean, etc.
      if (isSimpleObject(allInheritedTypes)) {
        return toJsonInner(
            objectValue.getValue(((ClassType) value.type()).fieldByName("value")), thread, refPath);
      }

      // Handle maps
      if (allInheritedTypes.contains("java.util.Map")) {
        return handleMap(objectValue, thread, refPath);
      }

      // Handle collections
      if (allInheritedTypes.contains("java.util.Collection")) {
        return handleCollection(objectValue, thread, refPath);
      }

      // Handle Java objects with overridden `toString`
      String javaObjectResult = handleJavaObject(objectValue, allInheritedTypes, thread, refPath);
      if (javaObjectResult != null) return javaObjectResult;

      // Handle general objects by iterating through their fields
      return handleObjectFields(objectValue, thread, refPath);
    }

    // Throw exception if value type is unsupported
    throw new JsonSerializeException("Unforeseen value type for : " + value.type().name());
  }

  /**
   * Checks if the time spent on the serialization process has exceeded the allowed limit. If the
   * time limit is exceeded, throws a JsonSerializeException.
   *
   * <p>This prevents the serialization of excessively large or complex objects.
   */
  private static void checkTimeLimit() {
    if (System.currentTimeMillis() - timeStamp > timeLimit) {
      throw new JsonSerializeException(
          "JSON serializing timed out, probably the object is too big to JSON.");
    }
  }

  /**
   * Detects circular references in the object graph to prevent infinite recursion.
   *
   * <p>If a circular reference is detected, this method ensures that it is not serialized again by
   * returning `true`.
   *
   * @param value the JDI value to check for circular references.
   * @param refPath a set containing the unique IDs of previously visited object references.
   * @return true if the value has already been visited (circular reference), otherwise false.
   */
  private static boolean isCircularReference(Value value, Set<Long> refPath) {
    if (value instanceof ObjectReference) {
      long id = ((ObjectReference) value).uniqueID();
      if (refPath.contains(id)) {
        return true; // Circular reference detected
      }
      refPath.add(id); // Add to visited references
    }
    return false;
  }

  /**
   * Serializes primitive and string values to their JSON representation.
   *
   * <p>Handles the basic Java types such as integers, doubles, floats, booleans, and strings. Also
   * escapes characters in strings to ensure valid JSON formatting.
   *
   * @param value the JDI value to serialize.
   * @return the JSON string representation of the value, or null if the value is not a primitive or
   *     string.
   */
  private static String handlePrimitiveAndStringValues(Value value) {
    if (value instanceof IntegerValue) return String.valueOf(((IntegerValue) value).value());
    if (value instanceof DoubleValue) return String.valueOf(((DoubleValue) value).value());
    if (value instanceof FloatValue) return String.valueOf(((FloatValue) value).value());
    if (value instanceof ShortValue) return String.valueOf(((ShortValue) value).value());
    if (value instanceof ByteValue) return String.valueOf(((ByteValue) value).value());
    if (value instanceof LongValue) return String.valueOf(((LongValue) value).value());
    if (value instanceof CharValue)
      return "\"" + escape(String.valueOf(((CharValue) value).value())) + "\"";
    if (value instanceof BooleanValue) return String.valueOf(((BooleanValue) value).value());
    if (value instanceof StringReference)
      return "\"" + escape(((StringReference) value).value()) + "\"";
    return null; // Not a primitive or string
  }

  /**
   * Serializes arrays to their JSON representation.
   *
   * <p>Recursively serializes each element of the array and combines them into a JSON array format.
   *
   * @param value the JDI value representing an array to serialize.
   * @param thread the current thread being used for method evaluation and serialization.
   * @param refPath a set containing the unique IDs of previously visited object references (for
   *     circular reference detection).
   * @return the JSON string representation of the array.
   */
  private static String handleArrayValues(Value value, ThreadReference thread, Set<Long> refPath) {
    ArrayReference arrayValue = (ArrayReference) value;
    StringBuilder str = new StringBuilder();
    str.append("[");
    for (Value v : arrayValue.getValues()) {
      str.append(toJsonInner(v, thread, refPath)); // Recursively serialize array elements
      str.append(",");
    }
    if (arrayValue.length() > 0) {
      str.delete(str.length() - 1, str.length()); // Remove trailing comma
    }
    str.append("]");
    return str.toString();
  }

  /**
   * Serializes a `java.util.Map` object to its JSON representation.
   *
   * <p>This method iterates through the map's key-value pairs, serializing each key and value to
   * JSON. Keys are checked for their type, with simple types directly converted and complex types
   * serialized using their string representation. Values are recursively serialized.
   *
   * @param objectValue the `ObjectReference` representing the map instance.
   * @param thread the current thread used for method invocation and serialization.
   * @param refPath a set containing the unique IDs of previously visited object references to
   *     prevent circular references.
   * @return the JSON string representation of the map.
   */
  private static String handleMap(
      ObjectReference objectValue, ThreadReference thread, Set<Long> refPath) {
    // Obtain the keySet of the map by invoking the "keySet" method
    ObjectReference keySet = (ObjectReference) invokeMethod(objectValue, "keySet", thread);
    if (keySet == null) {
      throw new JsonSerializeException(
          "KeySet of Map returns null: " + toValRefString(objectValue));
    }

    // Convert the keySet into an array for iteration
    ArrayReference keyArr = (ArrayReference) invokeMethod(keySet, "toArray", thread);
    if (keyArr == null) {
      throw new JsonSerializeException("KeySet convert failed: " + toValRefString(keySet));
    }

    // Build the JSON representation of the map
    StringBuilder str = new StringBuilder("{");
    for (Value key : keyArr.getValues()) {
      // Retrieve the value associated with the current key
      Value val = invokeMethod(objectValue, "get", thread, key);

      // Serialize the key to JSON
      String keyStr;
      if (isSimpleValue(key)) {
        String simpleValStr = toJsonInner(key, thread, refPath);
        keyStr =
            (simpleValStr != null && simpleValStr.startsWith("\""))
                ? simpleValStr
                : "\"" + simpleValStr + "\"";
      } else {
        keyStr = "\"" + toValRefString((ObjectReference) key) + "\"";
      }

      // Append the serialized key-value pair to the JSON string
      str.append(keyStr).append(":").append(toJsonInner(val, thread, refPath)).append(",");
    }

    // Remove the trailing comma, if present, and close the JSON object
    if (keyArr.length() > 0) {
      str.delete(str.length() - 1, str.length());
    }
    str.append("}");
    return str.toString();
  }

  /**
   * Serializes a `java.util.Collection` object to its JSON representation.
   *
   * <p>Converts the collection to an array using `toArray` and serializes it as a JSON array.
   *
   * @param objectValue the `ObjectReference` representing the collection.
   * @param thread the current thread being used for method evaluation and serialization.
   * @param refPath a set containing the unique IDs of previously visited object references.
   * @return the JSON string representation of the collection.
   */
  private static String handleCollection(
      ObjectReference objectValue, ThreadReference thread, Set<Long> refPath) {
    return toJsonInner(invokeMethod(objectValue, "toArray", thread), thread, refPath);
  }

  /**
   * Serializes a Java object using its `toString` method if overridden.
   *
   * <p>This method attempts to use the `toString` method for serialization if it has been
   * overridden by the object. If not overridden, it defaults to the object's type name and unique
   * ID as a simple string representation.
   *
   * @param objectValue the `ObjectReference` representing the Java object.
   * @param allInheritedTypes the set of all inherited types for the object, used to identify
   *     whether the object belongs to a standard Java package.
   * @param thread the current thread used for method invocation.
   * @param refPath a set containing the unique IDs of previously visited object references to
   *     prevent circular references.
   * @return the JSON string representation of the Java object, or `null` if unsupported.
   */
  private static String handleJavaObject(
      ObjectReference objectValue,
      Set<String> allInheritedTypes,
      ThreadReference thread,
      Set<Long> refPath) {
    // Iterate over the object's inherited types to determine its behavior
    for (String type : allInheritedTypes) {
      if (type.startsWith("java")) {
        // Check if the `toString` method is overridden (not the default from `Object`)
        boolean hasOverriddenToString =
            !objectValue
                .referenceType()
                .methodsByName("toString")
                .get(0)
                .declaringType()
                .name()
                .equals(JAVA_LANG_OBJECT);

        if (hasOverriddenToString) {
          // Use the result of the overridden `toString` method
          return toJsonInner(invokeMethod(objectValue, "toString", thread), thread, refPath);
        } else {
          // Default to a simple type and unique ID representation
          return "\"" + toValRefString(objectValue) + "\"";
        }
      }
    }
    return null; // No Java-related types found for this object
  }

  /**
   * Serializes an object by iterating through its fields and converting each field to JSON.
   *
   * <p>This method handles complex objects that are not maps, collections, or primitive wrappers.
   * It retrieves all fields of the object, serializes their names and values, and constructs a JSON
   * object with these key-value pairs.
   *
   * @param objectValue the `ObjectReference` representing the object to be serialized.
   * @param thread the current thread used for method invocation and serialization.
   * @param refPath a set containing the unique IDs of previously visited object references to
   *     prevent circular references.
   * @return the JSON string representation of the object's fields.
   */
  private static String handleObjectFields(
      ObjectReference objectValue, ThreadReference thread, Set<Long> refPath) {
    StringBuilder str = new StringBuilder("{");
    boolean hasOne = false; // Tracks if the object has any fields

    // Retrieve and iterate over all fields and their values
    for (Map.Entry<Field, Value> fieldValueEntry :
        objectValue.getValues(((ClassType) objectValue.type()).allFields()).entrySet()) {
      String fieldName = fieldValueEntry.getKey().name();
      String fieldValue = toJsonInner(fieldValueEntry.getValue(), thread, refPath);

      // Append the serialized field name and value to the JSON string
      str.append("\"").append(fieldName).append("\"").append(":").append(fieldValue).append(",");
      hasOne = true;
    }

    // Remove the trailing comma if any fields were added
    if (hasOne) {
      str.delete(str.length() - 1, str.length());
    }

    str.append("}"); // Close the JSON object
    return str.toString();
  }

  /**
   * Checks if a given set of inherited types belongs to a simple wrapper object.
   *
   * <p>A "simple wrapper object" is defined as one of the following Java wrapper types: - Integer,
   * Byte, Double, Float, Long, Short, Boolean, or Character.
   *
   * @param allInheritedTypes a set containing all inherited types for an object.
   * @return `true` if the object is a simple wrapper type, otherwise `false`.
   */
  private static boolean isSimpleObject(Set<String> allInheritedTypes) {
    return allInheritedTypes.contains("java.lang.Integer")
        || allInheritedTypes.contains("java.lang.Byte")
        || allInheritedTypes.contains("java.lang.Double")
        || allInheritedTypes.contains("java.lang.Float")
        || allInheritedTypes.contains("java.lang.Long")
        || allInheritedTypes.contains("java.lang.Short")
        || allInheritedTypes.contains("java.lang.Boolean")
        || allInheritedTypes.contains("java.lang.Character");
  }

  /**
   * Determines if a given `Value` is a simple value.
   *
   * <p>Simple values include: - Primitive values (e.g., `int`, `double`, `boolean`). - String
   * values. - Objects that are simple wrapper types (e.g., `Integer`, `Boolean`).
   *
   * @param value the `Value` to evaluate.
   * @return `true` if the value is simple, otherwise `false`.
   */
  private static boolean isSimpleValue(Value value) {
    if (value == null || value instanceof PrimitiveValue || value instanceof StringReference) {
      return true;
    } else if (value instanceof ObjectReference) {
      return isSimpleObject(getAllInheritedTypes(value));
    } else {
      return false;
    }
  }

  /**
   * Generates a string representation for an `ObjectReference`.
   *
   * <p>The string representation includes: - The object's type name. - The unique ID of the object.
   *
   * @param valRef the `ObjectReference` to represent as a string.
   * @return a formatted string representation of the object, including its type and unique ID.
   */
  @NotNull private static String toValRefString(ObjectReference valRef) {
    long id = valRef.uniqueID();
    return valRef.type().name() + "(id=" + id + ")";
  }

  /**
   * Retrieves all inherited types (class and interface names) for a given `Value`.
   *
   * <p>This method walks through the inheritance hierarchy of the value's type, collecting: - The
   * name of the value's immediate type. - All implemented interface types. - All parent
   * (superclass) types up to `java.lang.Object`.
   *
   * @param value the `Value` whose inheritance hierarchy is to be analyzed.
   * @return a set of all inherited type names (excluding `java.lang.Object`).
   */
  @NotNull private static Set<String> getAllInheritedTypes(Value value) {
    Set<String> allInheritedTypes = new HashSet<>();
    ClassType type = ((ClassType) value.type());

    // Add the name of the current type
    allInheritedTypes.add(type.name());

    // Add all implemented interfaces
    for (InterfaceType iType : type.allInterfaces()) {
      allInheritedTypes.add(iType.name());
    }

    // Traverse and add all superclasses
    while (type.superclass() != null) {
      type = type.superclass();
      allInheritedTypes.add(type.name());
    }

    // Exclude `java.lang.Object` from the result
    allInheritedTypes.remove(JAVA_LANG_OBJECT);

    return allInheritedTypes;
  }

  /**
   * Escapes special characters in a string to make it JSON-safe.
   *
   * <p>The method replaces characters that might interfere with JSON parsing, such as: -
   * Backslashes (`\`). - Double quotes (`"`). - Special control characters (`\b`, `\f`, `\n`, `\r`,
   * `\t`).
   *
   * @param raw the raw string to escape.
   * @return the escaped string, safe for inclusion in a JSON string.
   */
  private static String escape(String raw) {
    return raw.replace("\\", "\\\\") // Escape backslashes
        .replace("\"", "\\\"") // Escape double quotes
        .replace("\b", "\\b") // Escape backspace
        .replace("\f", "\\f") // Escape form feed
        .replace("\n", "\\n") // Escape newline
        .replace("\r", "\\r") // Escape carriage return
        .replace("\t", "\\t"); // Escape tab
  }
}
