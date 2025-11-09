package com.benx421.tweetaudit.analyzer;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;

/**
 * Production implementation of GeminiClient using the Google Generative AI SDK.
 */
public class GeminiSdkClient implements GeminiClient {

  private final Client client;
  private final String modelName;
  private final GenerateContentConfig config;

  /**
   * Creates a GeminiSdkClient with the specified API key and model.
   *
   * @param apiKey the Gemini API key
   * @param modelName the model name (e.g., "gemini-2.5-flash")
   */
  public GeminiSdkClient(String apiKey, String modelName) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalArgumentException("API key cannot be null or blank");
    }
    if (modelName == null || modelName.isBlank()) {
      throw new IllegalArgumentException("Model name cannot be null or blank");
    }

    this.client = Client.builder().apiKey(apiKey).build();
    this.modelName = modelName;
    this.config = GenerateContentConfig.builder().responseMimeType("application/json").build();
  }

  @Override
  public String generateContent(String prompt) throws Exception {
    if (prompt == null || prompt.isBlank()) {
      throw new IllegalArgumentException("Prompt cannot be null or blank");
    }

    GenerateContentResponse response = client.models.generateContent(modelName, prompt, config);

    String responseText = response.text();
    if (responseText == null || responseText.isBlank()) {
      throw new Exception("Empty response from Gemini API");
    }

    return responseText;
  }
}
