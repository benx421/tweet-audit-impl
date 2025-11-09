package com.benx421.tweetaudit.storage;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

import com.benx421.tweetaudit.models.AnalysisResult;
import com.benx421.tweetaudit.models.Tweet;

/**
 * Writes CSV files with automatic directory creation.
 * Must be used with try-with-resources to ensure proper resource cleanup.
 */
public final class CSVWriter implements AutoCloseable {

  private static final Set<PosixFilePermission> FILE_PERMISSIONS =
      Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

  private final BufferedWriter writer;
  private boolean skipHeader;

  private CSVWriter(Path path, boolean shouldAppend) throws IOException {

    Path dir = path.getParent();
    if (dir != null) {
      Files.createDirectories(dir);
    }

    boolean fileExists = Files.exists(path);
    this.skipHeader = shouldAppend && fileExists;
    this.writer = new BufferedWriter(new FileWriter(path.toFile(), shouldAppend));

    try {
      Files.setPosixFilePermissions(path, FILE_PERMISSIONS);
    } catch (UnsupportedOperationException e) {
      // POSIX permissions not supported on this file system (e.g., Windows)
    }
  }

  public static CSVWriter create(String path, boolean shouldAppend) throws IOException {
    if (path == null || path.isBlank()) {
      throw new IllegalArgumentException("Path cannot be null or blank");
    }
    return new CSVWriter(Paths.get(path).normalize(), shouldAppend);
  }

  public void writeTweets(List<Tweet> tweets) throws IOException {
    if (!skipHeader) {
      writer.write("id,text");
      writer.newLine();
    }

    for (Tweet tweet : tweets) {
      writer.write(escapeCsv(tweet.id()));
      writer.write(',');
      writer.write(escapeCsv(tweet.content()));
      writer.newLine();
    }

    writer.flush();
  }

  public void writeResult(AnalysisResult result) throws IOException {
    if (!skipHeader) {
      writer.write("tweet_url,deleted");
      writer.newLine();
      skipHeader = true;
    }

    writer.write(escapeCsv(result.tweetUrl()));
    writer.write(',');
    writer.write("false");
    writer.newLine();
    writer.flush();
  }

  @Override
  public void close() throws IOException {
    if (writer != null) {
      writer.close();
    }
  }

  private String escapeCsv(String value) {
    if (value == null) {
      return "";
    }

    if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    return value;
  }
}
