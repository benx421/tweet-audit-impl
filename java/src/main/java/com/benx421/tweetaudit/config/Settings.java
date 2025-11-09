package com.benx421.tweetaudit.config;

import java.time.Duration;

/**
 * Application configuration settings.
 */
public final class Settings {

  private final String tweetsArchivePath;
  private final String transformedTweetsPath;
  private final String checkpointPath;
  private final String processedResultsPath;
  private final String baseTwitterUrl;
  private final String username;
  private final String geminiApiKey;
  private final String geminiModel;
  private final int batchSize;
  private final Duration rateLimitDelay;
  private final Criteria criteria;

  private Settings(Builder builder) {
    this.tweetsArchivePath = builder.tweetsArchivePath;
    this.transformedTweetsPath = builder.transformedTweetsPath;
    this.checkpointPath = builder.checkpointPath;
    this.processedResultsPath = builder.processedResultsPath;
    this.baseTwitterUrl = builder.baseTwitterUrl;
    this.username = builder.username;
    this.geminiApiKey = builder.geminiApiKey;
    this.geminiModel = builder.geminiModel;
    this.batchSize = builder.batchSize;
    this.rateLimitDelay = builder.rateLimitDelay;
    this.criteria = builder.criteria;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String tweetsArchivePath() {
    return tweetsArchivePath;
  }

  public String transformedTweetsPath() {
    return transformedTweetsPath;
  }

  public String checkpointPath() {
    return checkpointPath;
  }

  public String processedResultsPath() {
    return processedResultsPath;
  }

  public String baseTwitterUrl() {
    return baseTwitterUrl;
  }

  public String username() {
    return username;
  }

  public String geminiApiKey() {
    return geminiApiKey;
  }

  public String geminiModel() {
    return geminiModel;
  }

  public int batchSize() {
    return batchSize;
  }

  public Duration rateLimitDelay() {
    return rateLimitDelay;
  }

  public Criteria criteria() {
    return criteria;
  }

  public String tweetUrl(String tweetId) {
    return baseTwitterUrl + "/" + username + "/status/" + tweetId;
  }

  public static final class Builder {
    private String tweetsArchivePath = "data/tweets/tweets.json";
    private String transformedTweetsPath = "data/tweets/transformed/tweets.csv";
    private String checkpointPath = "data/checkpoint.txt";
    private String processedResultsPath = "data/tweets/processed/results.csv";
    private String baseTwitterUrl = "https://x.com";
    private String username = "user";
    private String geminiApiKey = "";
    private String geminiModel = "gemini-2.5-flash";
    private int batchSize = 10;
    private Duration rateLimitDelay = Duration.ofSeconds(1);
    private Criteria criteria = Criteria.defaults();

    public Builder tweetsArchivePath(String path) {
      this.tweetsArchivePath = path;
      return this;
    }

    public Builder transformedTweetsPath(String path) {
      this.transformedTweetsPath = path;
      return this;
    }

    public Builder checkpointPath(String path) {
      this.checkpointPath = path;
      return this;
    }

    public Builder processedResultsPath(String path) {
      this.processedResultsPath = path;
      return this;
    }

    public Builder baseTwitterUrl(String url) {
      this.baseTwitterUrl = url;
      return this;
    }

    public Builder username(String username) {
      this.username = username;
      return this;
    }

    public Builder geminiApiKey(String apiKey) {
      this.geminiApiKey = apiKey;
      return this;
    }

    public Builder geminiModel(String model) {
      this.geminiModel = model;
      return this;
    }

    public Builder batchSize(int size) {
      this.batchSize = size;
      return this;
    }

    public Builder rateLimitDelay(Duration delay) {
      this.rateLimitDelay = delay;
      return this;
    }

    public Builder criteria(Criteria criteria) {
      this.criteria = criteria;
      return this;
    }

    public Settings build() {
      return new Settings(this);
    }
  }
}
