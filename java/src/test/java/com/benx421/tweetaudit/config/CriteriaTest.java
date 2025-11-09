package com.benx421.tweetaudit.config;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CriteriaTest {

  @Test
  void testCriteriaWithAllFields() {
    Criteria criteria =
        new Criteria(
            List.of("word1", "word2"),
            List.of("topic1", "topic2"),
            List.of("tone1", "tone2"),
            "Additional instructions");

    assertEquals(List.of("word1", "word2"), criteria.forbiddenWords());
    assertEquals(List.of("topic1", "topic2"), criteria.topicsToExclude());
    assertEquals(List.of("tone1", "tone2"), criteria.toneRequirements());
    assertEquals("Additional instructions", criteria.additionalInstructions());
  }

  @Test
  void testCriteriaWithNullFields() {
    Criteria criteria = new Criteria(null, null, null, null);

    assertTrue(criteria.forbiddenWords().isEmpty());
    assertTrue(criteria.topicsToExclude().isEmpty());
    assertTrue(criteria.toneRequirements().isEmpty());
    assertEquals("", criteria.additionalInstructions());
  }

  @Test
  void testCriteriaImmutability() {
    List<String> mutableWords = new ArrayList<>(List.of("word1"));
    Criteria criteria = new Criteria(mutableWords, List.of(), List.of(), "");

    mutableWords.add("word2");

    assertEquals(1, criteria.forbiddenWords().size());
    assertEquals("word1", criteria.forbiddenWords().get(0));
  }

  @Test
  void testCriteriaDefaultsFactory() {
    Criteria criteria = Criteria.defaults();

    assertTrue(criteria.forbiddenWords().isEmpty());
    assertEquals(3, criteria.topicsToExclude().size());
    assertEquals(2, criteria.toneRequirements().size());
    assertTrue(criteria.additionalInstructions().contains("professional reputation"));
  }

  @Test
  void testCriteriaReturnsUnmodifiableList() {
    Criteria criteria = new Criteria(List.of("word1"), List.of(), List.of(), "");

    assertThrows(
        UnsupportedOperationException.class, () -> criteria.forbiddenWords().add("word2"));
  }
}
