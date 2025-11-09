package com.benx421.tweetaudit.models;

/**
 * The analysis result for a single tweet.
 */
public record AnalysisResult(String tweetUrl, Decision decision) {

  public AnalysisResult {
    if (tweetUrl == null || tweetUrl.isBlank()) {
      throw new IllegalArgumentException("Tweet URL cannot be null or blank");
    }
    tweetUrl = tweetUrl.trim();
    if (decision == null) {
      decision = Decision.KEEP;
    }
  }

  public AnalysisResult(String tweetUrl) {
    this(tweetUrl, Decision.KEEP);
  }

  public boolean shouldDelete() {
    return decision == Decision.DELETE;
  }

  public boolean shouldKeep() {
    return decision == Decision.KEEP;
  }

  public static AnalysisResult forDeletion(String tweetUrl) {
    return new AnalysisResult(tweetUrl, Decision.DELETE);
  }

  public static AnalysisResult forKeeping(String tweetUrl) {
    return new AnalysisResult(tweetUrl, Decision.KEEP);
  }

  @Override
  public String toString() {
    return "AnalysisResult[tweetUrl=" + tweetUrl + ", decision=" + decision + "]";
  }
}
