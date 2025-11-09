package com.benx421.tweetaudit.models;

/**
 * A tweet from the Twitter archive.
 */
public record Tweet(String id, String content) {

  public Tweet {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("Tweet ID cannot be null or blank");
    }
    if (content == null) {
      throw new IllegalArgumentException("Tweet content cannot be null");
    }
    id = id.trim();
  }

  public String contentPreview() {
    return content.length() > 50 ? content.substring(0, 50) + "..." : content;
  }

  @Override
  public String toString() {
    return "Tweet[id=" + id + ", content=" + contentPreview() + "]";
  }

  public static Tweet of(String id, String content) {
    return new Tweet(id, content);
  }
}
