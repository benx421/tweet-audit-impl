package com.benx421.tweetaudit.models;

/**
 * Analysis decision for a tweet: KEEP or DELETE.
 */
public enum Decision {
  KEEP,
  DELETE;

  public static Decision fromString(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Decision value cannot be null or blank");
    }

    return switch (value.trim().toUpperCase()) {
      case "KEEP" -> KEEP;
      case "DELETE" -> DELETE;
      default ->
          throw new IllegalArgumentException(
              "Invalid decision value: " + value + ". Must be 'KEEP' or 'DELETE'");
    };
  }
}
