package com.benx421.tweetaudit.config;

import java.util.List;

/**
 * Evaluation criteria for tweet analysis.
 */
public record Criteria(
    List<String> forbiddenWords,
    List<String> topicsToExclude,
    List<String> toneRequirements,
    String additionalInstructions) {

  public Criteria {
    forbiddenWords = forbiddenWords == null ? List.of() : List.copyOf(forbiddenWords);
    topicsToExclude = topicsToExclude == null ? List.of() : List.copyOf(topicsToExclude);
    toneRequirements = toneRequirements == null ? List.of() : List.copyOf(toneRequirements);
    additionalInstructions = additionalInstructions == null ? "" : additionalInstructions;
  }

  public static Criteria defaults() {
    return new Criteria(
        List.of(),
        List.of(
            "Profanity or unprofessional language",
            "Personal attacks or insults",
            "Outdated political opinions"),
        List.of("Professional language only", "Respectful communication"),
        "Flag any content that could harm professional reputation");
  }
}
