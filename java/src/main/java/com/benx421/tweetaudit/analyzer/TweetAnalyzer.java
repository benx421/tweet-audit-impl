package com.benx421.tweetaudit.analyzer;

import com.benx421.tweetaudit.models.AnalysisResult;
import com.benx421.tweetaudit.models.Tweet;

/**
 * Analyzes tweets against configured criteria to determine if they should be deleted.
 */
public interface TweetAnalyzer {

  AnalysisResult analyze(Tweet tweet) throws AnalyzerException;
}
