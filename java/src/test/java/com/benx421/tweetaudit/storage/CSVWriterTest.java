package com.benx421.tweetaudit.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.benx421.tweetaudit.models.AnalysisResult;
import com.benx421.tweetaudit.models.Decision;
import com.benx421.tweetaudit.models.Tweet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CSVWriterTest {

  @Test
  void testWriteTweetsCreatesFile(@TempDir Path tempDir) throws IOException {
    Path csvFile = tempDir.resolve("tweets.csv");
    List<Tweet> tweets = List.of(
        new Tweet("123", "First tweet"),
        new Tweet("456", "Second tweet"));

    try (CSVWriter writer = CSVWriter.create(csvFile.toString(), false)) {
      writer.writeTweets(tweets);
    }

    assertTrue(Files.exists(csvFile));
    List<String> lines = Files.readAllLines(csvFile);
    assertEquals(3, lines.size());
    assertEquals("id,text", lines.get(0));
    assertEquals("123,First tweet", lines.get(1));
    assertEquals("456,Second tweet", lines.get(2));
  }

  @Test
  void testWriteTweetsWithSpecialCharacters(@TempDir Path tempDir) throws IOException {
    Path csvFile = tempDir.resolve("tweets.csv");
    List<Tweet> tweets = List.of(
        new Tweet("123", "Tweet with, comma"),
        new Tweet("456", "Tweet with \"quotes\""));

    try (CSVWriter writer = CSVWriter.create(csvFile.toString(), false)) {
      writer.writeTweets(tweets);
    }

    List<String> lines = Files.readAllLines(csvFile);
    assertEquals(3, lines.size());
    assertEquals("123,\"Tweet with, comma\"", lines.get(1));
    assertTrue(lines.get(2).contains("\"Tweet with \"\"quotes\"\"\""));
  }

  @Test
  void testWriteTweetsAppendMode(@TempDir Path tempDir) throws IOException {
    Path csvFile = tempDir.resolve("tweets.csv");

    try (CSVWriter writer = CSVWriter.create(csvFile.toString(), false)) {
      writer.writeTweets(List.of(new Tweet("123", "First")));
    }

    try (CSVWriter writer = CSVWriter.create(csvFile.toString(), true)) {
      writer.writeTweets(List.of(new Tweet("456", "Second")));
    }

    List<String> lines = Files.readAllLines(csvFile);
    assertEquals(3, lines.size());
    assertEquals("id,text", lines.get(0));
    assertEquals("123,First", lines.get(1));
    assertEquals("456,Second", lines.get(2));
  }

  @Test
  void testWriteResultCreatesFile(@TempDir Path tempDir) throws IOException {
    Path csvFile = tempDir.resolve("results.csv");
    AnalysisResult result = new AnalysisResult("https://x.com/user/status/123", Decision.DELETE);

    try (CSVWriter writer = CSVWriter.create(csvFile.toString(), false)) {
      writer.writeResult(result);
    }

    assertTrue(Files.exists(csvFile));
    List<String> lines = Files.readAllLines(csvFile);
    assertEquals(2, lines.size());
    assertEquals("tweet_url,deleted", lines.get(0));
    assertEquals("https://x.com/user/status/123,false", lines.get(1));
  }

  @Test
  void testWriteMultipleResults(@TempDir Path tempDir) throws IOException {
    Path csvFile = tempDir.resolve("results.csv");

    try (CSVWriter writer = CSVWriter.create(csvFile.toString(), false)) {
      writer.writeResult(new AnalysisResult("https://x.com/user/status/123", Decision.DELETE));
      writer.writeResult(new AnalysisResult("https://x.com/user/status/456", Decision.DELETE));
    }

    List<String> lines = Files.readAllLines(csvFile);
    assertEquals(3, lines.size());
    assertEquals("tweet_url,deleted", lines.get(0));
    assertEquals("https://x.com/user/status/123,false", lines.get(1));
    assertEquals("https://x.com/user/status/456,false", lines.get(2));
  }

  @Test
  void testWriteResultAppendMode(@TempDir Path tempDir) throws IOException {
    Path csvFile = tempDir.resolve("results.csv");

    try (CSVWriter writer = CSVWriter.create(csvFile.toString(), false)) {
      writer.writeResult(new AnalysisResult("https://x.com/user/status/123", Decision.DELETE));
    }

    try (CSVWriter writer = CSVWriter.create(csvFile.toString(), true)) {
      writer.writeResult(new AnalysisResult("https://x.com/user/status/456", Decision.DELETE));
    }

    List<String> lines = Files.readAllLines(csvFile);
    assertEquals(3, lines.size());
    assertEquals("tweet_url,deleted", lines.get(0));
    assertEquals("https://x.com/user/status/123,false", lines.get(1));
    assertEquals("https://x.com/user/status/456,false", lines.get(2));
  }

  @Test
  void testWriterCreatesDirectories(@TempDir Path tempDir) throws IOException {
    Path csvFile = tempDir.resolve("subdir/nested/tweets.csv");

    try (CSVWriter writer = CSVWriter.create(csvFile.toString(), false)) {
      writer.writeTweets(List.of(new Tweet("123", "Test")));
    }

    assertTrue(Files.exists(csvFile));
  }

  @Test
  void testCreateWithNullPath() {
    assertThrows(IllegalArgumentException.class, () -> CSVWriter.create(null, false));
  }

  @Test
  void testCreateWithBlankPath() {
    assertThrows(IllegalArgumentException.class, () -> CSVWriter.create("  ", false));
  }
}
