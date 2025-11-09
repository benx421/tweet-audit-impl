package com.benx421.tweetaudit.analyzer;

import java.time.Duration;

import com.benx421.tweetaudit.config.Criteria;
import com.benx421.tweetaudit.models.AnalysisResult;
import com.benx421.tweetaudit.models.Decision;
import com.benx421.tweetaudit.models.Tweet;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Analyzes tweets using Google Gemini AI.
 */
public class GeminiAnalyzer implements TweetAnalyzer {

  private final GeminiClient client;
  private final Criteria criteria;
  private final String username;
  private final String baseTwitterUrl;
  private final RateLimiter rateLimiter;
  private final RetryPolicy retryPolicy;
  private final Gson gson;

  public GeminiAnalyzer(
      GeminiClient client,
      Criteria criteria,
      String username,
      String baseTwitterUrl,
      Duration rateLimitDelay) {
    this.client = client;
    this.criteria = criteria;
    this.username = username;
    this.baseTwitterUrl = baseTwitterUrl;
    this.rateLimiter = new RateLimiter(rateLimitDelay);
    this.retryPolicy = RetryPolicy.withDefaults();
    this.gson = new Gson();
  }

  public static GeminiAnalyzer create(
      GeminiClient client, Criteria criteria, String username) {
    return new GeminiAnalyzer(
        client, criteria, username, "https://x.com", Duration.ofSeconds(1));
  }

  @Override
  public AnalysisResult analyze(Tweet tweet) throws AnalyzerException {
    try {
      rateLimiter.waitIfNeeded();

      String prompt = buildPrompt(tweet);

      String responseJson =
          retryPolicy.execute(
              () -> {
                String response = client.generateContent(prompt);
                if (response == null || response.isBlank()) {
                  throw new AnalyzerException("Empty response from Gemini for tweet " + tweet.id());
                }
                return response;
              });

      GeminiResponse geminiResponse = parseResponse(responseJson, tweet.id());
      if (geminiResponse == null) {
        throw new AnalyzerException("Failed to parse Gemini response for tweet " + tweet.id());
      }
      Decision decision = Decision.fromString(geminiResponse.decision);

      return new AnalysisResult(tweetUrl(tweet.id()), decision);

    } catch (AnalyzerException e) {
      throw e;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AnalyzerException("Analysis interrupted for tweet " + tweet.id(), e);
    } catch (Exception e) {
      throw new AnalyzerException("Failed to analyze tweet " + tweet.id(), e);
    }
  }

  private String buildPrompt(Tweet tweet) {
    StringBuilder criteriaList = new StringBuilder();

    int index = 1;
    for (String topic : criteria.topicsToExclude()) {
      criteriaList.append(index++).append(". ").append(topic).append("\n");
    }

    for (String tone : criteria.toneRequirements()) {
      criteriaList.append(index++).append(". ").append(tone).append("\n");
    }

    if (!criteria.forbiddenWords().isEmpty()) {
      String words = String.join(", ", criteria.forbiddenWords());
      criteriaList.append(index).append(". Contains any of these words: ").append(words).append("\n");
    }

    String additionalInstructions = "";
    if (!criteria.additionalInstructions().isBlank()) {
      additionalInstructions = "\n\nAdditional guidance: " + criteria.additionalInstructions();
    }

    return String.format(
        """
        You are evaluating tweets for a professional's Twitter cleanup.

        Tweet ID: %s
        Tweet: "%s"

        Mark for deletion if it violates any of these criteria:
        %s%s

        Respond in JSON format:
        {
          "decision": "DELETE" or "KEEP"
        }""",
        tweet.id(), tweet.content(), criteriaList.toString(), additionalInstructions);
  }

  private GeminiResponse parseResponse(String responseJson, String tweetId)
      throws AnalyzerException {
    try {
      GeminiResponse response = gson.fromJson(responseJson, GeminiResponse.class);

      if (response == null || response.decision == null || response.decision.isBlank()) {
        throw new AnalyzerException(
            "Invalid Gemini response for tweet "
                + tweetId
                + ": missing decision (response: "
                + responseJson
                + ")");
      }

      return response;
    } catch (JsonSyntaxException e) {
      throw new AnalyzerException(
          "Failed to parse Gemini response for tweet "
              + tweetId
              + ": "
              + e.getMessage()
              + " (response: "
              + responseJson
              + ")",
          e);
    }
  }

  private String tweetUrl(String tweetId) {
    return baseTwitterUrl + "/" + username + "/status/" + tweetId;
  }

  private static final class GeminiResponse {
    private String decision;
  }
}
