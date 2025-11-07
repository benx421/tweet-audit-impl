from unittest.mock import MagicMock, Mock, call, patch

import pytest

from application import Application
from models import AnalysisResult, Decision, Tweet


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


@pytest.mark.parametrize(
    "exception,expected_error_type,expected_message,context",
    [
        (FileNotFoundError("File not found"), "file_not_found", "File not found", "extraction"),
        (ValueError("Invalid format"), "invalid_format", "Invalid format", "analysis"),
        (PermissionError("Permission denied"), "permission_denied", "Permission denied", ""),
        (
            RuntimeError("Unexpected error"),
            "unexpected_error",
            "An unexpected error occurred.",
            "extraction",
        ),
    ],
)
def test_build_error_result(exception, expected_error_type, expected_message, context):
    result = Application._build_error_result(exception, context)

    assert result.success is False
    assert result.count == 0
    assert result.error_type == expected_error_type
    assert result.error_message == expected_message


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
@patch("application.CSVParser")
@patch("application.Checkpoint")
@patch("application.CSVWriter")
def test_should_analyze_tweets_successfully_with_batch_processing(
    mock_writer_class, mock_checkpoint_class, mock_parser_class, mock_settings
):
    mock_settings.transformed_tweets_path = "data/tweets.csv"
    mock_settings.checkpoint_path = "data/checkpoint.txt"
    mock_settings.processed_results_path = "data/results.csv"
    mock_settings.batch_size = 2

    tweets = [
        Tweet(id="1", content="tweet 1"),
        Tweet(id="2", content="tweet 2"),
        Tweet(id="3", content="tweet 3"),
    ]
    mock_parser = Mock()
    mock_parser.parse.return_value = tweets
    mock_parser_class.return_value = mock_parser

    mock_checkpoint = MagicMock()
    mock_checkpoint.load.return_value = 0
    mock_checkpoint_class.return_value.__enter__.return_value = mock_checkpoint

    mock_writer = MagicMock()
    mock_writer_class.return_value.__enter__.return_value = mock_writer

    mock_analyzer = Mock()
    mock_analyzer.analyze.side_effect = [
        AnalysisResult(tweet_url="url1", decision=Decision.DELETE),
        AnalysisResult(tweet_url="url2", decision=Decision.KEEP),
        AnalysisResult(tweet_url="url3", decision=Decision.DELETE),
    ]

    app = Application()
    app._analyzer = mock_analyzer

    app.analyze_tweets()

    mock_parser_class.assert_called_once_with("data/tweets.csv")
    mock_parser.parse.assert_called_once()
    assert mock_analyzer.analyze.call_count == 3
    assert mock_writer.write_result.call_count == 2  # Only DELETE results written
    assert mock_checkpoint.save.call_count == 2
    mock_checkpoint.save.assert_has_calls([call(2), call(3)])


@patch("application.settings")
@patch("application.CSVParser")
@patch("application.Checkpoint")
@patch("application.CSVWriter")
def test_should_resume_from_checkpoint(
    mock_writer_class, mock_checkpoint_class, mock_parser_class, mock_settings
):
    mock_settings.transformed_tweets_path = "data/tweets.csv"
    mock_settings.checkpoint_path = "data/checkpoint.txt"
    mock_settings.processed_results_path = "data/results.csv"
    mock_settings.batch_size = 2

    tweets = [
        Tweet(id="1", content="tweet 1"),
        Tweet(id="2", content="tweet 2"),
        Tweet(id="3", content="tweet 3"),
    ]
    mock_parser = Mock()
    mock_parser.parse.return_value = tweets
    mock_parser_class.return_value = mock_parser

    mock_checkpoint = MagicMock()
    mock_checkpoint.load.return_value = 2
    mock_checkpoint_class.return_value.__enter__.return_value = mock_checkpoint

    mock_writer = MagicMock()
    mock_writer_class.return_value.__enter__.return_value = mock_writer

    mock_analyzer = Mock()
    mock_analyzer.analyze.return_value = AnalysisResult(tweet_url="url3", decision=Decision.DELETE)

    app = Application()
    app._analyzer = mock_analyzer

    app.analyze_tweets()

    assert mock_analyzer.analyze.call_count == 1
    mock_analyzer.analyze.assert_called_once_with(tweets[2])
    mock_checkpoint.save.assert_called_once_with(3)


@patch("application.settings")
@patch("application.CSVParser")
@patch("application.Checkpoint")
@patch("application.CSVWriter")
def test_should_handle_empty_tweet_list(
    mock_writer_class, mock_checkpoint_class, mock_parser_class, mock_settings
):
    mock_settings.transformed_tweets_path = "data/tweets.csv"

    mock_parser = Mock()
    mock_parser.parse.return_value = []
    mock_parser_class.return_value = mock_parser

    mock_analyzer = Mock()

    app = Application()
    app._analyzer = mock_analyzer

    app.analyze_tweets()

    mock_analyzer.analyze.assert_not_called()
    mock_checkpoint_class.assert_not_called()
    mock_writer_class.assert_not_called()


@patch("application.settings")
@patch("application.CSVParser")
@patch("application.Checkpoint")
@patch("application.CSVWriter")
def test_should_return_error_when_analyzer_fails(
    mock_writer_class, mock_checkpoint_class, mock_parser_class, mock_settings
):
    mock_settings.transformed_tweets_path = "data/tweets.csv"
    mock_settings.checkpoint_path = "data/checkpoint.txt"
    mock_settings.processed_results_path = "data/results.csv"
    mock_settings.batch_size = 10

    tweets = [Tweet(id="1", content="test tweet")]
    mock_parser = Mock()
    mock_parser.parse.return_value = tweets
    mock_parser_class.return_value = mock_parser

    mock_checkpoint = MagicMock()
    mock_checkpoint.load.return_value = 0
    mock_checkpoint_class.return_value.__enter__.return_value = mock_checkpoint

    mock_writer = MagicMock()
    mock_writer_class.return_value.__enter__.return_value = mock_writer

    mock_analyzer = Mock()
    mock_analyzer.analyze.side_effect = ValueError("API error")

    app = Application()
    app._analyzer = mock_analyzer

    result = app.analyze_tweets()

    assert result.success is False
    assert result.count == 0
    assert result.error_type == "analysis_failed"
    assert "API error" in result.error_message
    mock_checkpoint.save.assert_not_called()
