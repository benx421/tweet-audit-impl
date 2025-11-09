package com.benx421.tweetaudit.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigLoaderTest {

  @Test
  void testLoadWithDefaultsWhenNoConfigFile() {
    ConfigLoader loader = new ConfigLoader();

    Settings settings = loader.load("nonexistent.json");

    assertNotNull(settings);
    assertEquals(Criteria.defaults(), settings.criteria());
    assertEquals("user", settings.username());
  }

  @Test
  void testLoadCriteriaFromValidFile(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("config.json");
    String json =
        """
        {
          "criteria": {
            "forbidden_words": ["word1", "word2"],
            "topics_to_exclude": ["Topic1", "Topic2"],
            "tone_requirements": ["Tone1"],
            "additional_instructions": "Custom instructions"
          }
        }
        """;
    Files.writeString(configFile, json);

    ConfigLoader loader = new ConfigLoader();
    Criteria criteria = loader.loadCriteriaFromFile(configFile.toString());

    assertNotNull(criteria);
    assertEquals(List.of("word1", "word2"), criteria.forbiddenWords());
    assertEquals(List.of("Topic1", "Topic2"), criteria.topicsToExclude());
    assertEquals(List.of("Tone1"), criteria.toneRequirements());
    assertEquals("Custom instructions", criteria.additionalInstructions());
  }

  @Test
  void testLoadCriteriaFromFileWithMissingFields(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("partial.json");
    String json =
        """
        {
          "criteria": {
            "forbidden_words": ["test"]
          }
        }
        """;
    Files.writeString(configFile, json);

    ConfigLoader loader = new ConfigLoader();
    Criteria criteria = loader.loadCriteriaFromFile(configFile.toString());

    assertNotNull(criteria);
    assertEquals(List.of("test"), criteria.forbiddenWords());
    assertTrue(criteria.topicsToExclude().isEmpty());
    assertTrue(criteria.toneRequirements().isEmpty());
    assertEquals("", criteria.additionalInstructions());
  }

  @Test
  void testLoadCriteriaFromNonexistentFile() {
    ConfigLoader loader = new ConfigLoader();

    Criteria criteria = loader.loadCriteriaFromFile("does-not-exist.json");

    assertNull(criteria);
  }

  @Test
  void testLoadCriteriaFromInvalidJson(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("invalid.json");
    Files.writeString(configFile, "not valid json{");

    ConfigLoader loader = new ConfigLoader();
    Criteria criteria = loader.loadCriteriaFromFile(configFile.toString());

    assertNull(criteria);
  }

  @Test
  void testLoadCriteriaFromEmptyFile(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("empty.json");
    Files.writeString(configFile, "{}");

    ConfigLoader loader = new ConfigLoader();
    Criteria criteria = loader.loadCriteriaFromFile(configFile.toString());

    assertNull(criteria);
  }

  @Test
  void testLoadOverridesCriteriaFromFile(@TempDir Path tempDir) throws IOException {
    Path configFile = tempDir.resolve("config.json");
    String json =
        """
        {
          "criteria": {
            "forbidden_words": ["custom"],
            "topics_to_exclude": ["CustomTopic"],
            "tone_requirements": ["CustomTone"],
            "additional_instructions": "Custom"
          }
        }
        """;
    Files.writeString(configFile, json);

    ConfigLoader loader = new ConfigLoader();
    Settings settings = loader.load(configFile.toString());

    assertEquals(List.of("custom"), settings.criteria().forbiddenWords());
    assertEquals(List.of("CustomTopic"), settings.criteria().topicsToExclude());
    assertEquals(List.of("CustomTone"), settings.criteria().toneRequirements());
    assertEquals("Custom", settings.criteria().additionalInstructions());
  }
}
