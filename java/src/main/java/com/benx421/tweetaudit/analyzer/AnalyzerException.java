package com.benx421.tweetaudit.analyzer;

/**
 * Exception thrown when tweet analysis fails.
 */
public class AnalyzerException extends Exception {

  public AnalyzerException(String message) {
    super(message);
  }

  public AnalyzerException(String message, Throwable cause) {
    super(message, cause);
  }
}
