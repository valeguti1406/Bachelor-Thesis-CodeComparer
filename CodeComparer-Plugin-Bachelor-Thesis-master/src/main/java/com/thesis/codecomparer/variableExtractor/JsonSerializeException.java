package com.thesis.codecomparer.variableExtractor;

/**
 * Custom exception class used to handle JSON serialization errors.
 *
 * This exception is thrown when the serialization process encounters issues,
 * such as unsupported value types or exceeding the allowed time limit.
 *
 * Extends `RuntimeException` to allow unchecked exceptions during runtime.
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
