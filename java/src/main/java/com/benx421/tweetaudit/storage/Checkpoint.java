package com.benx421.tweetaudit.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * Manages progress tracking for tweet analysis.
 */
public final class Checkpoint {

  private static final Set<PosixFilePermission> FILE_PERMISSIONS =
      Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

  private final Path path;

  public Checkpoint(String path) {
    if (path == null || path.isBlank()) {
      throw new IllegalArgumentException("Path cannot be null or blank");
    }
    this.path = Paths.get(path).normalize();
  }

  public int load() throws IOException {
    if (!Files.exists(path)) {
      return 0;
    }

    try {
      String content = Files.readString(path).trim();
      if (content.isEmpty()) {
        return 0;
      }
      return Integer.parseInt(content);
    } catch (NumberFormatException e) {
      throw new IOException("Invalid checkpoint file format: " + e.getMessage(), e);
    }
  }

  public void save(int index) throws IOException {
    if (index < 0) {
      throw new IllegalArgumentException("Index cannot be negative: " + index);
    }

    Path dir = path.getParent();
    if (dir != null) {
      Files.createDirectories(dir);
    }

    Files.writeString(path, String.valueOf(index));

    try {
      Files.setPosixFilePermissions(path, FILE_PERMISSIONS);
    } catch (UnsupportedOperationException e) {
      // POSIX permissions not supported on this file system (e.g., Windows)
    }
  }
}
