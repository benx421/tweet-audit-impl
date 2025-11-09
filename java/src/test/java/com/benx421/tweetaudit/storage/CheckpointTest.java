package com.benx421.tweetaudit.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckpointTest {

  @Test
  void testLoadNonexistentFileReturnsZero(@TempDir Path tempDir) throws IOException {
    Path checkpointFile = tempDir.resolve("checkpoint.txt");
    Checkpoint checkpoint = new Checkpoint(checkpointFile.toString());

    int index = checkpoint.load();

    assertEquals(0, index);
  }

  @Test
  void testSaveAndLoad(@TempDir Path tempDir) throws IOException {
    Path checkpointFile = tempDir.resolve("checkpoint.txt");
    Checkpoint checkpoint = new Checkpoint(checkpointFile.toString());

    checkpoint.save(42);
    int index = checkpoint.load();

    assertEquals(42, index);
    assertTrue(Files.exists(checkpointFile));
  }

  @Test
  void testSaveOverwritesExisting(@TempDir Path tempDir) throws IOException {
    Path checkpointFile = tempDir.resolve("checkpoint.txt");
    Checkpoint checkpoint = new Checkpoint(checkpointFile.toString());

    checkpoint.save(10);
    checkpoint.save(20);
    int index = checkpoint.load();

    assertEquals(20, index);
  }

  @Test
  void testSaveZeroIndex(@TempDir Path tempDir) throws IOException {
    Path checkpointFile = tempDir.resolve("checkpoint.txt");
    Checkpoint checkpoint = new Checkpoint(checkpointFile.toString());

    checkpoint.save(0);
    int index = checkpoint.load();

    assertEquals(0, index);
  }

  @Test
  void testSaveLargeIndex(@TempDir Path tempDir) throws IOException {
    Path checkpointFile = tempDir.resolve("checkpoint.txt");
    Checkpoint checkpoint = new Checkpoint(checkpointFile.toString());

    checkpoint.save(1000000);
    int index = checkpoint.load();

    assertEquals(1000000, index);
  }

  @Test
  void testLoadEmptyFileReturnsZero(@TempDir Path tempDir) throws IOException {
    Path checkpointFile = tempDir.resolve("checkpoint.txt");
    Files.writeString(checkpointFile, "");
    Checkpoint checkpoint = new Checkpoint(checkpointFile.toString());

    int index = checkpoint.load();

    assertEquals(0, index);
  }

  @Test
  void testLoadWhitespaceOnlyFile(@TempDir Path tempDir) throws IOException {
    Path checkpointFile = tempDir.resolve("checkpoint.txt");
    Files.writeString(checkpointFile, "  \n  ");
    Checkpoint checkpoint = new Checkpoint(checkpointFile.toString());

    int index = checkpoint.load();

    assertEquals(0, index);
  }

  @Test
  void testLoadInvalidFormatThrowsException(@TempDir Path tempDir) throws IOException {
    Path checkpointFile = tempDir.resolve("checkpoint.txt");
    Files.writeString(checkpointFile, "not_a_number");
    Checkpoint checkpoint = new Checkpoint(checkpointFile.toString());

    assertThrows(IOException.class, checkpoint::load);
  }

  @Test
  void testSaveCreatesDirectories(@TempDir Path tempDir) throws IOException {
    Path checkpointFile = tempDir.resolve("subdir/nested/checkpoint.txt");
    Checkpoint checkpoint = new Checkpoint(checkpointFile.toString());

    checkpoint.save(100);

    assertTrue(Files.exists(checkpointFile));
    assertEquals(100, checkpoint.load());
  }

  @Test
  void testSaveNegativeIndexThrowsException(@TempDir Path tempDir) {
    Path checkpointFile = tempDir.resolve("checkpoint.txt");
    Checkpoint checkpoint = new Checkpoint(checkpointFile.toString());

    assertThrows(IllegalArgumentException.class, () -> checkpoint.save(-1));
  }

  @Test
  void testConstructorWithNullPath() {
    assertThrows(IllegalArgumentException.class, () -> new Checkpoint(null));
  }

  @Test
  void testConstructorWithBlankPath() {
    assertThrows(IllegalArgumentException.class, () -> new Checkpoint("  "));
  }
}
