package com.benx421.tweetaudit.analyzer;

/**
 * Interface for Gemini AI client (allows mocking in tests).
 */
public interface GeminiClient {

  String generateContent(String prompt) throws Exception;
}
