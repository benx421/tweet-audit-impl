package com.benx421.tweetaudit.application;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import com.benx421.tweetaudit.analyzer.GeminiAnalyzer;
import com.benx421.tweetaudit.analyzer.TweetAnalyzer;
import com.benx421.tweetaudit.config.Settings;
import com.benx421.tweetaudit.models.AnalysisResult;
import com.benx421.tweetaudit.models.Decision;
import com.benx421.tweetaudit.models.Tweet;
import com.benx421.tweetaudit.storage.CSVWriter;
import com.benx421.tweetaudit.storage.Checkpoint;
import com.benx421.tweetaudit.storage.ParserType;
import com.benx421.tweetaudit.storage.TweetParser;

/**
 * Orchestrates tweet extraction and analysis workflows.
 * Coordinates the parser, analyzer, writer, and checkpoint components.
 */
public final class Application {

  private static final Logger LOGGER = Logger.getLogger(Application.class.getName());

  private final TweetAnalyzer analyzer;
  private final Settings settings;
  private final Checkpoint checkpoint;

  public Application(Settings settings) {
    if (settings == null) {
      throw new IllegalArgumentException("Settings cannot be null");
    }
    this.settings = settings;
    this.analyzer = GeminiAnalyzer.fromSettings(settings);
    this.checkpoint = new Checkpoint(settings.checkpointPath());
  }

  // Package-private constructor for testing with mock analyzer
  Application(Settings settings, TweetAnalyzer analyzer) {
    if (settings == null) {
      throw new IllegalArgumentException("Settings cannot be null");
    }
    if (analyzer == null) {
      throw new IllegalArgumentException("Analyzer cannot be null");
    }
    this.settings = settings;
    this.analyzer = analyzer;
    this.checkpoint = new Checkpoint(settings.checkpointPath());
  }

  /**
   * Parses the Twitter archive JSON and exports tweets to CSV format.
   * This is the first step in the workflow, transforming raw archive data into a processable
   * format.
   *
   * @throws IOException if an I/O error occurs during parsing or writing
   */
  public void extractTweets() throws IOException {
    LOGGER.info("Reading tweets from " + settings.tweetsArchivePath());
    TweetParser parser = new TweetParser(settings.tweetsArchivePath(), ParserType.JSON);

    List<Tweet> tweets;
    try {
      tweets = parser.parse();
    } catch (IOException e) {
      LOGGER.severe(
          "Failed to parse tweets from "
              + settings.tweetsArchivePath()
              + ": "
              + e.getMessage());
      throw new IOException(
          "Failed to parse tweets from "
              + settings.tweetsArchivePath()
              + " (file may not be valid JSON): "
              + e.getMessage(),
          e);
    }

    LOGGER.info("Extracted " + tweets.size() + " tweets, writing to CSV");

    try (CSVWriter writer = CSVWriter.create(settings.transformedTweetsPath(), false)) {
      writer.writeTweets(tweets);
    } catch (IOException e) {
      LOGGER.severe("Error writing tweets to CSV: " + e.getMessage());
      throw new IOException("Error writing tweets to CSV: " + e.getMessage(), e);
    }

    LOGGER.info(
        "Successfully wrote "
            + tweets.size()
            + " tweets to "
            + settings.transformedTweetsPath());
    System.out.printf("Extracted %d tweets to %s%n", tweets.size(), settings.transformedTweetsPath());
  }

  /**
   * Processes one batch of tweets using the Gemini analyzer.
   * Resumes from the last checkpoint, analyzes batchSize tweets, writes flagged results,
   * and saves progress.
   *
   * @throws IOException if an I/O error occurs during analysis
   */
  public void analyzeTweets() throws IOException {
    LOGGER.info("Loading tweets from " + settings.transformedTweetsPath());
    List<Tweet> tweets;
    try {
      tweets = parseTransformedTweets();
    } catch (IOException e) {
      LOGGER.severe("Failed to parse transformed tweets: " + e.getMessage());
      throw new IOException("Failed to parse transformed tweets: " + e.getMessage(), e);
    }

    if (tweets.isEmpty()) {
      LOGGER.warning("No tweets found to analyze");
      return;
    }

    LOGGER.info("Loaded " + tweets.size() + " tweets for analysis");

    int startIdx;
    try {
      startIdx = checkpoint.load();
    } catch (IOException e) {
      LOGGER.severe("Failed to load checkpoint: " + e.getMessage());
      throw new IOException("Failed to load checkpoint: " + e.getMessage(), e);
    }

    LOGGER.info("Resuming from tweet index " + startIdx);

    if (startIdx >= tweets.size()) {
      LOGGER.info("All tweets already analyzed");
      System.out.println("All tweets already analyzed");
      return;
    }

    int endIdx = Math.min(startIdx + settings.batchSize(), tweets.size());

    LOGGER.info(
        String.format(
            "Processing batch (tweets %d-%d of %d)", startIdx + 1, endIdx, tweets.size()));
    System.out.printf("Processing tweets %d to %d (total: %d)%n", startIdx, endIdx - 1, tweets.size());

    int analyzedCount = 0;
    int deleteCandidates = 0;

    try (CSVWriter writer = CSVWriter.create(settings.processedResultsPath(), true)) {
      for (int i = startIdx; i < endIdx; i++) {
        Tweet tweet = tweets.get(i);

        if (isRetweet(tweet)) {
          continue;
        }

        AnalysisResult result;
        try {
          result = analyzer.analyze(tweet);
          analyzedCount++;
          LOGGER.fine("Tweet " + tweet.id() + ": " + result.decision());
        } catch (Exception e) {
          LOGGER.severe("Failed to analyze tweet " + tweet.id() + ": " + e.getMessage());
          throw new IOException("Failed to analyze tweet " + tweet.id() + ": " + e.getMessage(), e);
        }

        if (result.decision() == Decision.DELETE) {
          deleteCandidates++;
          writer.writeResult(result);
        }
      }
    } catch (IOException e) {
      LOGGER.severe("Failed to write results: " + e.getMessage());
      throw new IOException("Failed to write results: " + e.getMessage(), e);
    }

    try {
      checkpoint.save(endIdx);
      LOGGER.info("Checkpoint saved at index " + endIdx);
    } catch (IOException e) {
      LOGGER.severe("Error saving checkpoint: " + e.getMessage());
      throw new IOException("Error saving checkpoint: " + e.getMessage(), e);
    }

    LOGGER.info(
        String.format(
            "Batch complete! Analyzed %d tweets, found %d deletion candidates (%d/%d total)",
            analyzedCount, deleteCandidates, endIdx, tweets.size()));
    LOGGER.info("Results written to " + settings.processedResultsPath());
    System.out.printf(
        "Batch complete! Processed %d tweets (%d/%d total)%n",
        endIdx - startIdx, endIdx, tweets.size());
  }

  private List<Tweet> parseTransformedTweets() throws IOException {
    TweetParser parser = new TweetParser(settings.transformedTweetsPath(), ParserType.CSV);
    return parser.parse();
  }

  /**
   * Checks if a tweet is a retweet by looking for "RT @" prefix.
   *
   * @param tweet the tweet to check
   * @return true if the tweet is a retweet, false otherwise
   */
  private static boolean isRetweet(Tweet tweet) {
    String content = tweet.content();
    return content != null && content.startsWith("RT @");
  }
}
