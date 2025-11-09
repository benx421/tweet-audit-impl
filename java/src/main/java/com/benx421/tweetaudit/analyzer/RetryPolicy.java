package com.benx421.tweetaudit.analyzer;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Retry policy with exponential backoff for transient errors.
 */
class RetryPolicy {

  private static final Set<String> RETRYABLE_KEYWORDS =
      Set.of("timeout", "connection", "rate limit", "quota", "503", "429", "temporarily unavailable");

  private final int maxRetries;
  private final Duration initialDelay;

  RetryPolicy(int maxRetries, Duration initialDelay) {
    if (maxRetries < 1) {
      throw new IllegalArgumentException("maxRetries must be at least 1, got: " + maxRetries);
    }
    this.maxRetries = maxRetries;
    this.initialDelay = initialDelay;
  }

  public static RetryPolicy withDefaults() {
    return new RetryPolicy(3, Duration.ofSeconds(1));
  }

  <T> T execute(Callable<T> operation) throws Exception {
    for (int attempt = 0; attempt < maxRetries; attempt++) {
      try {
        return operation.call();
      } catch (Exception e) {
        if (!isRetryable(e) || attempt == maxRetries - 1) {
          throw e;
        }

        long sleepMillis = calculateBackoff(attempt);
        Thread.sleep(sleepMillis);
      }
    }

    throw new IllegalStateException("Retry loop completed without returning or throwing");
  }

  private boolean isRetryable(Exception e) {
    String errorMessage = e.getMessage();
    if (errorMessage == null) {
      return false;
    }

    String lowerMessage = errorMessage.toLowerCase();
    return RETRYABLE_KEYWORDS.stream().anyMatch(lowerMessage::contains);
  }

  private long calculateBackoff(int attempt) {
    long exponentialDelay = initialDelay.toMillis() * (1L << attempt);
    long jitter = (long) (Math.random() * 1000);
    return exponentialDelay + jitter;
  }
}
