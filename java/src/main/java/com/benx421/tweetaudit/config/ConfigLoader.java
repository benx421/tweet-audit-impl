package com.benx421.tweetaudit.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

/**
 * Loads configuration from environment variables and config files.
 */
public final class ConfigLoader {

  private static final String DEFAULT_CONFIG_FILE = "config.json";
  private final Gson gson;

  public ConfigLoader() {
    this.gson = new Gson();
  }

  public Settings load() {
    return load(DEFAULT_CONFIG_FILE);
  }

  public Settings load(String configFilePath) {
    Settings.Builder builder = Settings.builder();

    loadFromEnvironment(builder);

    Criteria criteria = loadCriteriaFromFile(configFilePath);
    if (criteria != null) {
      builder.criteria(criteria);
    }

    return builder.build();
  }

  private void loadFromEnvironment(Settings.Builder builder) {
    getEnv("X_USERNAME").ifPresent(builder::username);
    getEnv("GEMINI_API_KEY").ifPresent(builder::geminiApiKey);
    getEnv("GEMINI_MODEL").ifPresent(builder::geminiModel);
    getEnv("BATCH_SIZE").ifPresent(value -> builder.batchSize(Integer.parseInt(value)));
    getEnv("RATE_LIMIT_SECONDS")
        .ifPresent(value -> builder.rateLimitDelay(Duration.ofMillis((long) (Double.parseDouble(value) * 1000))));
    getEnv("TWEETS_ARCHIVE_PATH").ifPresent(builder::tweetsArchivePath);
    getEnv("TRANSFORMED_TWEETS_PATH").ifPresent(builder::transformedTweetsPath);
    getEnv("CHECKPOINT_PATH").ifPresent(builder::checkpointPath);
    getEnv("PROCESSED_RESULTS_PATH").ifPresent(builder::processedResultsPath);
  }

  Criteria loadCriteriaFromFile(String filePath) {
    try {
      Path path = Paths.get(filePath);
      if (!Files.exists(path)) {
        return null;
      }

      String json = Files.readString(path);
      ConfigFile configFile = gson.fromJson(json, ConfigFile.class);

      if (configFile == null || configFile.criteria == null) {
        return null;
      }

      return new Criteria(
          configFile.criteria.forbiddenWords != null ? configFile.criteria.forbiddenWords : List.of(),
          configFile.criteria.topicsToExclude != null ? configFile.criteria.topicsToExclude : List.of(),
          configFile.criteria.toneRequirements != null ? configFile.criteria.toneRequirements : List.of(),
          configFile.criteria.additionalInstructions != null ? configFile.criteria.additionalInstructions : "");

    } catch (IOException | JsonSyntaxException e) {
      // Config file is optional. Silently fall back to defaults if not found or invalid
      return null;
    }
  }

  private java.util.Optional<String> getEnv(String key) {
    String value = System.getenv(key);
    return value != null && !value.isBlank() ? java.util.Optional.of(value) : java.util.Optional.empty();
  }

  private static final class ConfigFile {
    CriteriaJson criteria;

    private static final class CriteriaJson {
      @SerializedName("forbidden_words")
      List<String> forbiddenWords;

      @SerializedName("topics_to_exclude")
      List<String> topicsToExclude;

      @SerializedName("tone_requirements")
      List<String> toneRequirements;

      @SerializedName("additional_instructions")
      String additionalInstructions;
    }
  }
}
