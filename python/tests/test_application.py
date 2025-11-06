from unittest.mock import MagicMock, Mock, patch

import pytest

from application import Application
from models import Tweet


def test_should_initialize_application_without_analyzer():
    app = Application()

    assert app._analyzer is None


@patch("application.Gemini")
def test_should_lazily_initialize_analyzer_when_accessed(mock_gemini_class):
    mock_gemini = Mock()
    mock_gemini_class.return_value = mock_gemini

    app = Application()
    assert app._analyzer is None

    analyzer = app.analyzer

    assert analyzer == mock_gemini
    mock_gemini_class.assert_called_once()

    analyzer2 = app.analyzer
    assert analyzer2 == mock_gemini
    mock_gemini_class.assert_called_once()


@patch("application.Gemini")
def test_should_raise_error_when_analyzer_accessed_without_api_key(mock_gemini_class):
    mock_gemini_class.side_effect = ValueError("GEMINI_API_KEY is required")

    app = Application()

    with pytest.raises(ValueError, match="GEMINI_API_KEY is required"):
        _ = app.analyzer


@patch("application.settings")
@patch("application.JSONParser")
@patch("application.CSVWriter")
def test_should_extract_tweets_successfully(mock_writer_class, mock_parser_class, mock_settings):
    mock_settings.tweets_archive_path = "data/tweets.json"
    mock_settings.transformed_tweets_path = "data/tweets.csv"

    mock_parser = Mock()
    mock_parser.parse.return_value = [
        Tweet(id="123", content="test tweet 1"),
        Tweet(id="456", content="test tweet 2"),
    ]
    mock_parser_class.return_value = mock_parser

    mock_writer = MagicMock()
    mock_writer_class.return_value.__enter__.return_value = mock_writer

    with patch("application.Gemini"):
        app = Application()
        result = app.extract_tweets()

    assert result.success is True
    assert result.count == 2
    assert result.error_type == ""
    assert result.error_message == ""

    mock_parser_class.assert_called_once_with("data/tweets.json")
    mock_parser.parse.assert_called_once()
    mock_writer.write_tweets.assert_called_once()


@patch("application.settings")
@patch("application.JSONParser")
def test_should_return_error_when_archive_not_found(mock_parser_class, mock_settings):
    mock_settings.tweets_archive_path = "data/nonexistent.json"

    mock_parser = Mock()
    mock_parser.parse.side_effect = FileNotFoundError(
        "Tweet archive not found: data/nonexistent.json"
    )
    mock_parser_class.return_value = mock_parser

    with patch("application.Gemini"):
        app = Application()
        result = app.extract_tweets()

    assert result.success is False
    assert result.count == 0
    assert result.error_type == "archive_not_found"
    assert "Tweet archive not found" in result.error_message


@patch("application.settings")
@patch("application.JSONParser")
def test_should_return_error_when_archive_has_invalid_format(mock_parser_class, mock_settings):
    mock_settings.tweets_archive_path = "data/invalid.json"

    mock_parser = Mock()
    mock_parser.parse.side_effect = ValueError("Invalid JSON in data/invalid.json")
    mock_parser_class.return_value = mock_parser

    with patch("application.Gemini"):
        app = Application()
        result = app.extract_tweets()

    assert result.success is False
    assert result.count == 0
    assert result.error_type == "invalid_format"
    assert "Invalid JSON" in result.error_message


@patch("application.settings")
@patch("application.JSONParser")
@patch("application.CSVWriter")
def test_should_return_error_when_permission_denied(
    mock_writer_class, mock_parser_class, mock_settings
):
    mock_settings.tweets_archive_path = "data/tweets.json"
    mock_settings.transformed_tweets_path = "/restricted/tweets.csv"

    mock_parser = Mock()
    mock_parser.parse.return_value = [Tweet(id="123", content="test")]
    mock_parser_class.return_value = mock_parser

    mock_writer_class.return_value.__enter__.side_effect = PermissionError(
        "Permission denied: /restricted/tweets.csv"
    )

    with patch("application.Gemini"):
        app = Application()
        result = app.extract_tweets()

    assert result.success is False
    assert result.count == 0
    assert result.error_type == "permission_denied"
    assert "Permission denied" in result.error_message


@patch("application.settings")
@patch("application.JSONParser")
def test_should_return_error_when_unexpected_exception_occurs(mock_parser_class, mock_settings):
    mock_settings.tweets_archive_path = "data/tweets.json"

    mock_parser = Mock()
    mock_parser.parse.side_effect = RuntimeError("Unexpected database error")
    mock_parser_class.return_value = mock_parser

    with patch("application.Gemini"):
        app = Application()
        result = app.extract_tweets()

    assert result.success is False
    assert result.count == 0
    assert result.error_type == "unexpected_error"
    assert result.error_message == "An unexpected error occurred."
