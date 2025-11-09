package com.benx421.tweetaudit;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {
  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;

  @BeforeEach
  void setUpStreams() {
    System.setOut(new PrintStream(outputStream));
    System.setErr(new PrintStream(errorStream));
  }

  @AfterEach
  void restoreStreams() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  @Test
  void testMainWithNoArgumentsPrintsUsage() {
    Main.main(new String[] {});

    String output = outputStream.toString();
    assertTrue(output.contains("Usage: tweet-audit <command>"), "Should print usage header");
    assertTrue(output.contains("extract-tweets"), "Should mention extract-tweets command");
    assertTrue(output.contains("analyze-tweets"), "Should mention analyze-tweets command");
    assertTrue(
        output.contains("Extract tweets from Twitter archive"),
        "Should describe extract-tweets");
    assertTrue(output.contains("Analyze tweets using Gemini AI"), "Should describe analyze-tweets");
  }

  @Test
  void testMainUsageFormat() {
    Main.main(new String[] {});

    String output = outputStream.toString();
    assertTrue(output.contains("Commands:"), "Should have Commands section");
    String[] lines = output.split(System.lineSeparator());
    assertTrue(lines.length >= 5, "Should have at least 5 lines of usage text");
  }
}
