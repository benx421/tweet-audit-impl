package com.benx421.tweetaudit.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.benx421.tweetaudit.analyzer.AnalyzerException;
import com.benx421.tweetaudit.analyzer.TweetAnalyzer;
import com.benx421.tweetaudit.config.Criteria;
import com.benx421.tweetaudit.config.Settings;
import com.benx421.tweetaudit.models.AnalysisResult;
import com.benx421.tweetaudit.models.Decision;
import com.benx421.tweetaudit.models.Tweet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationTest {

  @Test
  void testExtractTweetsFromJsonArchive(@TempDir Path tempDir) throws IOException {
    Path archivePath = tempDir.resolve("archive.json");
    Path outputPath = tempDir.resolve("tweets.csv");

    Files.writeString(
        archivePath,
        """
        [
          {"tweet": {"id_str": "123", "full_text": "First tweet"}},
          {"tweet": {"id_str": "456", "full_text": "Second tweet"}}
        ]
        """);

    Settings settings = buildSettings(tempDir, archivePath.toString(), outputPath.toString());
    Application app = new Application(settings);
    app.extractTweets();

    List<String> lines = Files.readAllLines(outputPath);
    assertEquals(3, lines.size());
    assertEquals("id,text", lines.get(0));
    assertEquals("123,First tweet", lines.get(1));
  }

  @Test
  void testExtractTweetsWithInvalidJson(@TempDir Path tempDir) throws IOException {
    Path archivePath = tempDir.resolve("invalid.json");
    Files.writeString(archivePath, "not valid json");

    Settings settings =
        buildSettings(tempDir, archivePath.toString(), tempDir.resolve("tweets.csv").toString());
    Application app = new Application(settings);

    IOException thrown = assertThrows(IOException.class, app::extractTweets);
    assertTrue(thrown.getMessage().contains("may not be valid JSON"));
  }

  @Test
  void testAnalyzeTweetsWritesOnlyDeleteDecisions(@TempDir Path tempDir) throws IOException {
    Path tweetsPath = tempDir.resolve("tweets.csv");
    Path resultsPath = tempDir.resolve("results.csv");
    Path checkpointPath = tempDir.resolve("checkpoint.txt");

    Files.writeString(
        tweetsPath,
        """
        id,text
        123,First tweet
        456,Second tweet
        """);

    Settings settings =
        Settings.builder()
            .geminiApiKey("fake-api-key")
            .tweetsArchivePath(tempDir.resolve("archive.json").toString())
            .transformedTweetsPath(tweetsPath.toString())
            .processedResultsPath(resultsPath.toString())
            .checkpointPath(checkpointPath.toString())
            .batchSize(2)
            .criteria(Criteria.defaults())
            .build();

    List<AnalysisResult> mockResults = new ArrayList<>();
    mockResults.add(new AnalysisResult("https://x.com/user/status/123", Decision.DELETE));
    mockResults.add(new AnalysisResult("https://x.com/user/status/456", Decision.KEEP));

    MockAnalyzer mockAnalyzer = new MockAnalyzer(mockResults);
    Application app = new Application(settings, mockAnalyzer);
    app.analyzeTweets();

    assertEquals(2, mockAnalyzer.getAnalyzeCount());

    List<String> lines = Files.readAllLines(resultsPath);
    assertEquals(2, lines.size());
    assertEquals("tweet_url,deleted", lines.get(0));
    assertEquals("https://x.com/user/status/123,false", lines.get(1));

    String checkpoint = Files.readString(checkpointPath).trim();
    assertEquals("2", checkpoint);
  }

  @Test
  void testAnalyzeTweetsResumesFromCheckpoint(@TempDir Path tempDir) throws IOException {
    Path tweetsPath = tempDir.resolve("tweets.csv");
    Path resultsPath = tempDir.resolve("results.csv");
    Path checkpointPath = tempDir.resolve("checkpoint.txt");

    Files.writeString(
        tweetsPath,
        """
        id,text
        1,Tweet 1
        2,Tweet 2
        3,Tweet 3
        """);
    Files.writeString(checkpointPath, "1");

    Settings settings =
        Settings.builder()
            .geminiApiKey("fake-api-key")
            .tweetsArchivePath(tempDir.resolve("archive.json").toString())
            .transformedTweetsPath(tweetsPath.toString())
            .processedResultsPath(resultsPath.toString())
            .checkpointPath(checkpointPath.toString())
            .batchSize(2)
            .criteria(Criteria.defaults())
            .build();

    List<AnalysisResult> mockResults = new ArrayList<>();
    mockResults.add(new AnalysisResult("https://x.com/user/status/2", Decision.DELETE));
    mockResults.add(new AnalysisResult("https://x.com/user/status/3", Decision.DELETE));

    MockAnalyzer mockAnalyzer = new MockAnalyzer(mockResults);
    Application app = new Application(settings, mockAnalyzer);
    app.analyzeTweets();

    assertEquals(2, mockAnalyzer.getAnalyzeCount());

    List<String> lines = Files.readAllLines(resultsPath);
    assertEquals(3, lines.size());

    String checkpoint = Files.readString(checkpointPath).trim();
    assertEquals("3", checkpoint);
  }

  @Test
  void testAnalyzeTweetsAllAlreadyProcessed(@TempDir Path tempDir) throws IOException {
    Path tweetsPath = tempDir.resolve("tweets.csv");
    Path checkpointPath = tempDir.resolve("checkpoint.txt");

    Files.writeString(
        tweetsPath,
        """
        id,text
        123,First tweet
        """);
    Files.writeString(checkpointPath, "1");

    Settings settings =
        buildSettings(tempDir, tempDir.resolve("archive.json").toString(), tweetsPath.toString());

    MockAnalyzer mockAnalyzer = new MockAnalyzer(new ArrayList<>());
    Application app = new Application(settings, mockAnalyzer);
    app.analyzeTweets();

    assertEquals(0, mockAnalyzer.getAnalyzeCount());
  }

  @Test
  void testConstructorValidation() {
    assertThrows(IllegalArgumentException.class, () -> new Application(null));

    Settings settings =
        Settings.builder()
            .tweetsArchivePath("archive.json")
            .transformedTweetsPath("tweets.csv")
            .build();
    assertThrows(IllegalArgumentException.class, () -> new Application(settings, null));
  }

  private Settings buildSettings(Path tempDir, String archivePath, String tweetsPath) {
    return Settings.builder()
        .geminiApiKey("fake-api-key")
        .tweetsArchivePath(archivePath)
        .transformedTweetsPath(tweetsPath)
        .processedResultsPath(tempDir.resolve("results.csv").toString())
        .checkpointPath(tempDir.resolve("checkpoint.txt").toString())
        .batchSize(10)
        .criteria(Criteria.defaults())
        .build();
  }

  private static class MockAnalyzer implements TweetAnalyzer {
    private final List<AnalysisResult> results;
    private int analyzeCount = 0;

    MockAnalyzer(List<AnalysisResult> results) {
      this.results = results;
    }

    @Override
    public AnalysisResult analyze(Tweet tweet) throws AnalyzerException {
      if (analyzeCount >= results.size()) {
        throw new IllegalStateException("No more mock results available");
      }
      return results.get(analyzeCount++);
    }

    int getAnalyzeCount() {
      return analyzeCount;
    }
  }
}
