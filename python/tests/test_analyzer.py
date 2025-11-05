from unittest.mock import Mock, patch

import pytest

from analyzer import Gemini
from config import Criteria
from models import Tweet


def test_should_analyze_tweet_with_delete_decision(mock_settings):
    with patch("analyzer.settings", mock_settings), patch("analyzer.genai") as mock_genai:
        mock_model = Mock()
        mock_response = Mock()
        mock_response.text = '{"decision": "DELETE", "reason": "Contains profanity"}'
        mock_model.generate_content.return_value = mock_response
        mock_genai.GenerativeModel.return_value = mock_model

        analyzer = Gemini()
        tweet = Tweet(id="123", content="Test tweet")

        result = analyzer.analyze(tweet)

        assert result.should_delete is True
        assert "123" in result.tweet_url
        assert "testuser" in result.tweet_url


def test_should_analyze_tweet_with_keep_decision(mock_settings):
    with patch("analyzer.settings", mock_settings), patch("analyzer.genai") as mock_genai:
        mock_model = Mock()
        mock_response = Mock()
        mock_response.text = '{"decision": "KEEP", "reason": "Professional content"}'
        mock_model.generate_content.return_value = mock_response
        mock_genai.GenerativeModel.return_value = mock_model

        analyzer = Gemini()
        tweet = Tweet(id="456", content="Professional tweet")

        result = analyzer.analyze(tweet)

        assert result.should_delete is False
        assert "456" in result.tweet_url


def test_should_raise_error_when_api_fails(mock_settings):
    with patch("analyzer.settings", mock_settings), patch("analyzer.genai") as mock_genai:
        mock_model = Mock()
        mock_model.generate_content.side_effect = RuntimeError("API connection failed")
        mock_genai.GenerativeModel.return_value = mock_model

        analyzer = Gemini()
        tweet = Tweet(id="999", content="Test")

        with pytest.raises(RuntimeError, match="API connection failed"):
            analyzer.analyze(tweet)


def test_should_raise_error_when_response_empty(mock_settings):
    with patch("analyzer.settings", mock_settings), patch("analyzer.genai") as mock_genai:
        mock_model = Mock()
        mock_response = Mock()
        mock_response.text = ""
        mock_model.generate_content.return_value = mock_response
        mock_genai.GenerativeModel.return_value = mock_model

        analyzer = Gemini()
        tweet = Tweet(id="111", content="Test")

        with pytest.raises(ValueError, match="Empty response"):
            analyzer.analyze(tweet)


def test_should_raise_error_when_response_invalid_json(mock_settings):
    with patch("analyzer.settings", mock_settings), patch("analyzer.genai") as mock_genai:
        mock_model = Mock()
        mock_response = Mock()
        mock_response.text = "not valid json{"
        mock_model.generate_content.return_value = mock_response
        mock_genai.GenerativeModel.return_value = mock_model

        analyzer = Gemini()
        tweet = Tweet(id="222", content="Test")

        with pytest.raises((ValueError, KeyError)):
            analyzer.analyze(tweet)


def test_should_build_prompt_with_all_criteria(mock_settings):
    mock_settings.criteria = Criteria(
        forbidden_words=["badword1", "badword2"],
        topics_to_exclude=["Political opinions", "Controversial statements"],
        tone_requirements=["Professional language", "Respectful communication"],
        additional_instructions="Flag harmful content",
    )

    with patch("analyzer.settings", mock_settings), patch("analyzer.genai") as mock_genai:
        mock_model = Mock()
        mock_response = Mock()
        mock_response.text = '{"decision": "KEEP", "reason": "OK"}'
        mock_model.generate_content.return_value = mock_response
        mock_genai.GenerativeModel.return_value = mock_model

        analyzer = Gemini()
        tweet = Tweet(id="123456789", content="This is a test tweet")

        analyzer.analyze(tweet)

        call_args = mock_model.generate_content.call_args
        prompt = call_args[0][0]

        assert "123456789" in prompt
        assert "This is a test tweet" in prompt
        assert "Political opinions" in prompt
        assert "Professional language" in prompt
        assert "badword1" in prompt
        assert "badword2" in prompt
        assert "Flag harmful content" in prompt
        assert '"decision": "DELETE" or "KEEP"' in prompt


def test_should_build_prompt_with_empty_criteria(mock_settings):
    mock_settings.criteria = Criteria(
        forbidden_words=[],
        topics_to_exclude=[],
        tone_requirements=[],
        additional_instructions="",
    )

    with patch("analyzer.settings", mock_settings), patch("analyzer.genai") as mock_genai:
        mock_model = Mock()
        mock_response = Mock()
        mock_response.text = '{"decision": "KEEP", "reason": "OK"}'
        mock_model.generate_content.return_value = mock_response
        mock_genai.GenerativeModel.return_value = mock_model

        analyzer = Gemini()
        tweet = Tweet(id="123456789", content="This is a test tweet")

        analyzer.analyze(tweet)

        call_args = mock_model.generate_content.call_args
        prompt = call_args[0][0]

        assert "123456789" in prompt
        assert "This is a test tweet" in prompt
        assert "You are evaluating tweets" in prompt
        assert "Additional guidance" not in prompt


def test_should_raise_error_when_api_key_missing(mock_settings):
    mock_settings.gemini_api_key = ""

    with patch("analyzer.settings", mock_settings):
        with pytest.raises(ValueError, match="GEMINI_API_KEY is required"):
            Gemini()
