package com.benx421.tweetaudit;

import java.io.IOException;

import com.benx421.tweetaudit.application.Application;
import com.benx421.tweetaudit.config.ConfigLoader;
import com.benx421.tweetaudit.config.Settings;

/**
 * Main entry point for the Tweet Audit application.
 */
public final class Main {

  private static final String EXTRACT_TWEETS_COMMAND = "extract-tweets";
  private static final String ANALYZE_TWEETS_COMMAND = "analyze-tweets";

  private Main() {
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      printUsage();
      return;
    }

    String command = args[0];

    if (!EXTRACT_TWEETS_COMMAND.equals(command) && !ANALYZE_TWEETS_COMMAND.equals(command)) {
      System.err.println("Error: Unknown command '" + command + "'");
      printUsage();
      System.exit(1);
    }

    try {
      ConfigLoader configLoader = new ConfigLoader();
      Settings settings = configLoader.load();
      Application app = new Application(settings);

      switch (command) {
        case EXTRACT_TWEETS_COMMAND -> executeExtractTweets(app);
        case ANALYZE_TWEETS_COMMAND -> executeAnalyzeTweets(app);
      }

    } catch (IOException e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    }
  }

  private static void executeExtractTweets(Application app) throws IOException {
    System.out.println("Extracting tweets from archive...");
    app.extractTweets();
    System.out.println("Successfully extracted tweets");
  }

  private static void executeAnalyzeTweets(Application app) throws IOException {
    System.out.println("Analyzing tweets...");
    app.analyzeTweets();
    System.out.println("Analysis complete!");
  }

  private static void printUsage() {
    System.out.println("Usage: tweet-audit <command>");
    System.out.println();
    System.out.println("Commands:");
    System.out.println("  extract-tweets  Extract tweets from Twitter archive");
    System.out.println("  analyze-tweets  Analyze tweets using Gemini AI");
  }
}
