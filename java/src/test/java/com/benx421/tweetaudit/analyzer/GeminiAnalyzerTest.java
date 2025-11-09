package com.benx421.tweetaudit.analyzer;

import java.time.Duration;

import com.benx421.tweetaudit.config.Criteria;
import com.benx421.tweetaudit.models.AnalysisResult;
import com.benx421.tweetaudit.models.Decision;
import com.benx421.tweetaudit.models.Tweet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiAnalyzerTest {

  private static class MockGeminiClient implements GeminiClient {
    private String response;

    MockGeminiClient withResponse(String response) {
      this.response = response;
      return this;
    }

    @Override
    public String generateContent(String prompt) {
      return response;
    }
  }

  @Test
  void testAnalyzeWithDeleteDecision() throws AnalyzerException {
    MockGeminiClient client =
        new MockGeminiClient()
            .withResponse("{\"decision\": \"DELETE\", \"reason\": \"Contains profanity\"}");

    GeminiAnalyzer analyzer =
        new GeminiAnalyzer(
            client, Criteria.defaults(), "testuser", "https://x.com", Duration.ofMillis(1));

    Tweet tweet = new Tweet("123456", "Test tweet");
    AnalysisResult result = analyzer.analyze(tweet);

    assertEquals(Decision.DELETE, result.decision());
    assertTrue(result.tweetUrl().contains("123456"));
    assertTrue(result.tweetUrl().contains("testuser"));
  }

  @Test
  void testAnalyzeWithKeepDecision() throws AnalyzerException {
    MockGeminiClient client =
        new MockGeminiClient()
            .withResponse("{\"decision\": \"KEEP\", \"reason\": \"Professional content\"}");

    GeminiAnalyzer analyzer = GeminiAnalyzer.create(client, Criteria.defaults(), "testuser");

    Tweet tweet = new Tweet("789", "Professional tweet");
    AnalysisResult result = analyzer.analyze(tweet);

    assertEquals(Decision.KEEP, result.decision());
  }

  @Test
  void testAnalyzeCaseInsensitiveDecision() throws AnalyzerException {
    MockGeminiClient client =
        new MockGeminiClient().withResponse("{\"decision\": \"delete\", \"reason\": \"test\"}");

    GeminiAnalyzer analyzer = GeminiAnalyzer.create(client, Criteria.defaults(), "testuser");

    Tweet tweet = new Tweet("111", "Test");
    AnalysisResult result = analyzer.analyze(tweet);

    assertEquals(Decision.DELETE, result.decision());
  }

  @Test
  void testAnalyzeWithEmptyResponse() {
    MockGeminiClient client = new MockGeminiClient().withResponse("");

    GeminiAnalyzer analyzer = GeminiAnalyzer.create(client, Criteria.defaults(), "testuser");

    Tweet tweet = new Tweet("222", "Test");
    assertThrows(AnalyzerException.class, () -> analyzer.analyze(tweet));
  }

  @Test
  void testAnalyzeWithInvalidJson() {
    MockGeminiClient client = new MockGeminiClient().withResponse("not valid json");

    GeminiAnalyzer analyzer = GeminiAnalyzer.create(client, Criteria.defaults(), "testuser");

    Tweet tweet = new Tweet("333", "Test");
    assertThrows(AnalyzerException.class, () -> analyzer.analyze(tweet));
  }

  @Test
  void testAnalyzeWithMissingDecisionField() {
    MockGeminiClient client = new MockGeminiClient().withResponse("{\"reason\": \"test\"}");

    GeminiAnalyzer analyzer = GeminiAnalyzer.create(client, Criteria.defaults(), "testuser");

    Tweet tweet = new Tweet("444", "Test");
    assertThrows(AnalyzerException.class, () -> analyzer.analyze(tweet));
  }

  @Test
  void testAnalyzeWithInvalidDecisionValue() {
    MockGeminiClient client =
        new MockGeminiClient().withResponse("{\"decision\": \"INVALID\", \"reason\": \"test\"}");

    GeminiAnalyzer analyzer = GeminiAnalyzer.create(client, Criteria.defaults(), "testuser");

    Tweet tweet = new Tweet("555", "Test");
    assertThrows(AnalyzerException.class, () -> analyzer.analyze(tweet));
  }

}
