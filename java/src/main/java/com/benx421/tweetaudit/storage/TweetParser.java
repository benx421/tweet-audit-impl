package com.benx421.tweetaudit.storage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.benx421.tweetaudit.models.Tweet;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

/**
 * Parses Twitter archive files into Tweet objects.
 */
public final class TweetParser {

  private final Path path;
  private final ParserType parserType;
  private final Gson gson;

  public TweetParser(String path, ParserType parserType) {
    if (path == null || path.isBlank()) {
      throw new IllegalArgumentException("Path cannot be null or blank");
    }
    if (parserType == null) {
      throw new IllegalArgumentException("ParserType cannot be null");
    }
    this.path = Paths.get(path).normalize();
    this.parserType = parserType;
    this.gson = new Gson();
  }

  public List<Tweet> parse() throws IOException {
    return switch (parserType) {
      case JSON -> parseJson();
      case CSV -> parseCsv();
    };
  }

  private List<Tweet> parseJson() throws IOException {
    try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
      TweetWrapper[] wrappers = gson.fromJson(reader, TweetWrapper[].class);

      if (wrappers == null) {
        throw new IOException("Failed to parse JSON: empty or invalid content");
      }

      List<Tweet> tweets = new ArrayList<>(wrappers.length);
      for (TweetWrapper wrapper : wrappers) {
        if (wrapper == null || wrapper.tweet == null) {
          throw new IOException("Invalid tweet wrapper: missing tweet data");
        }
        if (wrapper.tweet.id == null || wrapper.tweet.fullText == null) {
          throw new IOException("Invalid tweet data: missing id or full_text");
        }
        tweets.add(new Tweet(wrapper.tweet.id, wrapper.tweet.fullText));
      }

      return tweets;

    } catch (JsonSyntaxException e) {
      throw new IOException("Invalid JSON format: " + e.getMessage(), e);
    }
  }

  private List<Tweet> parseCsv() throws IOException {
    List<Tweet> tweets = new ArrayList<>(1024);

    try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
      String headerLine = reader.readLine();
      if (headerLine == null) {
        throw new IOException("CSV file is empty");
      }

      String[] headers = parseCsvLine(headerLine);
      if (headers.length < 2 || !headers[0].equals("id") || !headers[1].equals("text")) {
        throw new IOException("Invalid CSV format: expected 'id,text' headers");
      }

      int lineNum = 2;
      String line;
      while ((line = reader.readLine()) != null) {
        String[] fields = parseCsvLine(line);

        if (fields.length != 2) {
          throw new IOException(
              "Invalid CSV record at line " + lineNum + ": expected 2 fields, got " + fields.length);
        }

        tweets.add(new Tweet(fields[0], fields[1]));
        lineNum++;
      }
    }

    return tweets;
  }

  private String[] parseCsvLine(String line) {
    List<String> fields = new ArrayList<>();
    StringBuilder field = new StringBuilder(300);
    boolean inQuotes = false;

    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);

      if (c == '"') {
        if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
          field.append('"');
          i++;
        } else {
          inQuotes = !inQuotes;
        }
      } else if (c == ',' && !inQuotes) {
        fields.add(field.toString());
        field.setLength(0);
      } else {
        field.append(c);
      }
    }
    fields.add(field.toString());

    return fields.toArray(new String[0]);
  }

  private static final class TweetWrapper {
    private TweetData tweet;

    private static final class TweetData {
      @SerializedName("id_str")
      private String id;

      @SerializedName("full_text")
      private String fullText;
    }
  }
}
