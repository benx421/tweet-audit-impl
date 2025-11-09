package com.benx421.tweetaudit.analyzer;

import java.time.Duration;

/**
 * Simple rate limiter to enforce minimum delay between operations.
 */
class RateLimiter {

  private final Duration minInterval;
  private long lastRequestTimeNanos;

  RateLimiter(Duration minInterval) {
    this.minInterval = minInterval;
    this.lastRequestTimeNanos = 0;
  }

  synchronized void waitIfNeeded() throws InterruptedException {
    long now = System.nanoTime();
    long elapsedNanos = now - lastRequestTimeNanos;
    long requiredNanos = minInterval.toNanos();

    if (elapsedNanos < requiredNanos) {
      long sleepNanos = requiredNanos - elapsedNanos;
      long sleepMillis = (sleepNanos + 999_999) / 1_000_000;
      Thread.sleep(sleepMillis);
      lastRequestTimeNanos = System.nanoTime();
    } else {
      lastRequestTimeNanos = now;
    }
  }
}
