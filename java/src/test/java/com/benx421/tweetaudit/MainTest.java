package com.benx421.tweetaudit;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {
  private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;

  @BeforeEach
  void setUpStreams() {
    System.setOut(new PrintStream(outputStream));
  }

  @AfterEach
  void restoreStreams() {
    System.setOut(originalOut);
  }

  @Test
  void testMainPrintsWelcomeMessage() {
    Main.main(new String[] {});

    String output = outputStream.toString();
    assertTrue(
        output.contains("Welcome to Tweet Audit"), "Output should contain welcome message");
  }

  @Test
  void testMainOutputFormat() {
    Main.main(new String[] {});

    String output = outputStream.toString().trim();
    assertEquals("Welcome to Tweet Audit", output, "Output should exactly match expected message");
  }
}
