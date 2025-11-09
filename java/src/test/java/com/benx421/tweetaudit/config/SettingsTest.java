package com.benx421.tweetaudit.config;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingsTest {

  @Test
  void testDefaultSettings() {
    Settings settings = Settings.builder().build();

    assertEquals("data/tweets/tweets.json", settings.tweetsArchivePath());
    assertEquals("data/tweets/transformed/tweets.csv", settings.transformedTweetsPath());
    assertEquals("data/checkpoint.txt", settings.checkpointPath());
    assertEquals("data/tweets/processed/results.csv", settings.processedResultsPath());
    assertEquals("https://x.com", settings.baseTwitterUrl());
    assertEquals("user", settings.username());
    assertEquals("", settings.geminiApiKey());
    assertEquals("gemini-2.5-flash", settings.geminiModel());
    assertEquals(10, settings.batchSize());
    assertEquals(Duration.ofSeconds(1), settings.rateLimitDelay());
    assertEquals(Criteria.defaults(), settings.criteria());
  }

  @Test
  void testBuilderWithCustomValues() {
    Criteria customCriteria = new Criteria(
        List.of("bad"), List.of("topic"), List.of("tone"), "instructions");

    Settings settings =
        Settings.builder()
            .username("testuser")
            .geminiApiKey("test-key")
            .geminiModel("test-model")
            .batchSize(20)
            .rateLimitDelay(Duration.ofMillis(500))
            .criteria(customCriteria)
            .build();

    assertEquals("testuser", settings.username());
    assertEquals("test-key", settings.geminiApiKey());
    assertEquals("test-model", settings.geminiModel());
    assertEquals(20, settings.batchSize());
    assertEquals(Duration.ofMillis(500), settings.rateLimitDelay());
    assertEquals(customCriteria, settings.criteria());
  }

  @Test
  void testTweetUrl() {
    Settings settings = Settings.builder().username("testuser").build();

    String url = settings.tweetUrl("123456789");

    assertEquals("https://x.com/testuser/status/123456789", url);
  }

  @Test
  void testBuilderChaining() {
    Settings settings =
        Settings.builder()
            .username("user1")
            .geminiApiKey("key1")
            .batchSize(5)
            .build();

    assertEquals("user1", settings.username());
    assertEquals("key1", settings.geminiApiKey());
    assertEquals(5, settings.batchSize());
  }
}
