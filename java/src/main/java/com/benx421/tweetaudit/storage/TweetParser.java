package com.benx421.tweetaudit.storage;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.benx421.tweetaudit.models.Tweet;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

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
    try (Reader reader = new FileReader(path.toFile())) {
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
    try (Reader reader = new FileReader(path.toFile());
        CSVParser parser = new CSVParser(reader, CSVFormat.RFC4180.withFirstRecordAsHeader())) {

      // Validate headers
      if (parser.getHeaderNames().isEmpty()) {
        throw new IOException("CSV file is empty");
      }
      if (!parser.getHeaderNames().equals(List.of("id", "text"))) {
        throw new IOException("Invalid CSV format: expected 'id,text' headers");
      }

      List<Tweet> tweets = new ArrayList<>(1024);
      for (CSVRecord record : parser) {
        if (record.size() != 2) {
          throw new IOException(
              "Invalid CSV record at line "
                  + record.getRecordNumber()
                  + ": expected 2 fields, got "
                  + record.size());
        }

        tweets.add(new Tweet(record.get("id"), record.get("text")));
      }

      return tweets;
    }
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
