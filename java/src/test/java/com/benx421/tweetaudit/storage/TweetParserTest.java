package com.benx421.tweetaudit.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.benx421.tweetaudit.models.Tweet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TweetParserTest {

  @Test
  void testParseJsonFromArchive() throws IOException {
    String path = getClass().getResource("/storage/tweets.json").getPath();
    TweetParser parser = new TweetParser(path, ParserType.JSON);

    List<Tweet> tweets = parser.parse();

    assertEquals(4, tweets.size());
    assertEquals("1234567890123456789", tweets.get(0).id());
    assertEquals("#golang error handling is growing on me. Explicit is better than implicit after all.",
        tweets.get(0).content());
    assertEquals("9876543210987654321", tweets.get(1).id());
    assertEquals("@randomuser Great article on distributed systems! https://t.co/abcdef12345",
        tweets.get(1).content());
  }

  @Test
  void testParseEmptyJsonArray() throws IOException {
    String path = getClass().getResource("/storage/empty.json").getPath();
    TweetParser parser = new TweetParser(path, ParserType.JSON);

    List<Tweet> tweets = parser.parse();

    assertTrue(tweets.isEmpty());
  }

  @Test
  void testParseInvalidJson() {
    String path = getClass().getResource("/storage/invalid.json").getPath();
    TweetParser parser = new TweetParser(path, ParserType.JSON);

    assertThrows(IOException.class, parser::parse);
  }

  @Test
  void testParseNonexistentFile() {
    TweetParser parser = new TweetParser("/nonexistent/tweets.json", ParserType.JSON);

    assertThrows(IOException.class, parser::parse);
  }

  @Test
  void testParseCsvFile(@TempDir Path tempDir) throws IOException {
    Path csvFile = tempDir.resolve("tweets.csv");
    Files.writeString(csvFile, """
        id,text
        123,Simple tweet
        456,"Tweet with comma, in text"
        789,"Tweet with ""quotes"" in text"
        """);

    TweetParser parser = new TweetParser(csvFile.toString(), ParserType.CSV);
    List<Tweet> tweets = parser.parse();

    assertEquals(3, tweets.size());
    assertEquals("123", tweets.get(0).id());
    assertEquals("Simple tweet", tweets.get(0).content());
    assertEquals("456", tweets.get(1).id());
    assertEquals("Tweet with comma, in text", tweets.get(1).content());
    assertEquals("789", tweets.get(2).id());
    assertEquals("Tweet with \"quotes\" in text", tweets.get(2).content());
  }

  @Test
  void testParseCsvWithInvalidHeaders(@TempDir Path tempDir) throws IOException {
    Path csvFile = tempDir.resolve("invalid.csv");
    Files.writeString(csvFile, "wrong,headers\n123,test\n");

    TweetParser parser = new TweetParser(csvFile.toString(), ParserType.CSV);

    IOException exception = assertThrows(IOException.class, parser::parse);
    assertTrue(exception.getMessage().contains("Invalid CSV format"));
  }

  @Test
  void testParseCsvWithInvalidRecordLength(@TempDir Path tempDir) throws IOException {
    Path csvFile = tempDir.resolve("invalid.csv");
    Files.writeString(csvFile, "id,text\n123,test,extra\n");

    TweetParser parser = new TweetParser(csvFile.toString(), ParserType.CSV);

    IOException exception = assertThrows(IOException.class, parser::parse);
    assertTrue(exception.getMessage().contains("expected 2 fields"));
  }

  @Test
  void testParseCsvEmptyFile(@TempDir Path tempDir) throws IOException {
    Path csvFile = tempDir.resolve("empty.csv");
    Files.writeString(csvFile, "");

    TweetParser parser = new TweetParser(csvFile.toString(), ParserType.CSV);

    IOException exception = assertThrows(IOException.class, parser::parse);
    assertTrue(exception.getMessage().contains("empty"));
  }

  @Test
  void testConstructorWithNullPath() {
    assertThrows(IllegalArgumentException.class, () -> new TweetParser(null, ParserType.JSON));
  }

  @Test
  void testConstructorWithBlankPath() {
    assertThrows(IllegalArgumentException.class, () -> new TweetParser("  ", ParserType.JSON));
  }

  @Test
  void testConstructorWithNullParserType() {
    assertThrows(IllegalArgumentException.class, () -> new TweetParser("test.json", null));
  }
}
