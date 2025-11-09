package com.benx421.tweetaudit.models;

import java.util.Optional;

/**
 * Operation result with success/failure status, count, and error details.
 */
public record Result(boolean success, int count, String errorType, String errorMessage) {

  public Result {
    if (count < 0) {
      throw new IllegalArgumentException("Count cannot be negative: " + count);
    }
    errorType = errorType == null ? "" : errorType.trim();
    errorMessage = errorMessage == null ? "" : errorMessage.trim();

    if (!success && errorType.isEmpty()) {
      throw new IllegalArgumentException("Failed results must have an error type");
    }
    if (success && (!errorType.isEmpty() || !errorMessage.isEmpty())) {
      throw new IllegalArgumentException("Successful results cannot have error information");
    }
  }

  public static Result ok(int count) {
    return new Result(true, count, "", "");
  }

  public static Result ok() {
    return ok(0);
  }

  public static Result failure(String errorType, String errorMessage) {
    if (errorType == null || errorType.isBlank()) {
      throw new IllegalArgumentException("Error type cannot be null or blank for failures");
    }
    return new Result(false, 0, errorType, errorMessage == null ? "" : errorMessage);
  }

  public static Result failure(String errorType) {
    return failure(errorType, "");
  }

  public static Result failure(Exception exception) {
    String errorType = exception.getClass().getSimpleName();
    String errorMessage = exception.getMessage() != null ? exception.getMessage() : "";
    return failure(errorType, errorMessage);
  }

  public boolean isFailure() {
    return !success;
  }

  public Optional<String> getErrorType() {
    return errorType.isEmpty() ? Optional.empty() : Optional.of(errorType);
  }

  public Optional<String> getErrorMessage() {
    return errorMessage.isEmpty() ? Optional.empty() : Optional.of(errorMessage);
  }

  @Override
  public String toString() {
    if (success) {
      return "Result[success=true, count=" + count + "]";
    }
    return "Result[success=false, errorType=" + errorType + ", errorMessage=" + errorMessage + "]";
  }
}
