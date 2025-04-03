package com.thesis.codecomparer.variableSerializer;

/**
 * Custom exception class used to handle JSON serialization errors.
 *
 * <p>This exception is thrown when the serialization process encounters issues, such as unsupported
 * value types or exceeding the allowed time limit.
 *
 * <p>Extends `RuntimeException` to allow unchecked exceptions during runtime.
 *
 * <p>This class is adapted from the Debug Variable Extractor project by chocovon, available at: <a
 * href="https://github.com/chocovon/debug-variable-extractor">Github</a>
 *
 * <p>Original license and copyright apply.
 */
public class JsonSerializeException extends RuntimeException {

  /**
   * Constructs a new `JsonSerializeException` with the specified error message.
   *
   * @param message the error message describing the cause of the exception.
   */
  public JsonSerializeException(String message) {
    super(message);
  }
}
