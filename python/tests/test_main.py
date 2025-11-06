import sys
from unittest.mock import MagicMock, patch

import pytest

import main
from models import ExtractionResult


def test_should_show_help_when_no_args_provided(capsys):
    sys.argv = ["main.py"]
    main.main()
    captured = capsys.readouterr()
    assert "usage:" in captured.out


@patch("main.Application")
def test_should_extract_tweets_when_extract_command_given(mock_app_class, capsys):
    mock_app = MagicMock()
    mock_app.extract_tweets.return_value = ExtractionResult(success=True, count=42)
    mock_app_class.return_value = mock_app

    sys.argv = ["main.py", "extract-tweets"]
    main.main()

    captured = capsys.readouterr()
    assert "Extracting tweets from archive..." in captured.out
    assert "Successfully extracted 42 tweets" in captured.out
    mock_app.extract_tweets.assert_called_once()


@patch("main.Application")
def test_should_analyze_tweets_when_analyze_command_given(mock_app_class, capsys):
    mock_app = MagicMock()
    mock_app_class.return_value = mock_app

    sys.argv = ["main.py", "analyze-tweets"]
    main.main()

    captured = capsys.readouterr()
    assert "Analyzing tweets..." in captured.out
    mock_app.analyze_tweets.assert_called_once()


@patch("main.Application")
def test_should_handle_analyzer_initialization_error_gracefully(mock_app_class, capsys):
    mock_app = MagicMock()
    mock_app.analyze_tweets.side_effect = ValueError("GEMINI_API_KEY is required")
    mock_app_class.return_value = mock_app

    sys.argv = ["main.py", "analyze-tweets"]

    with pytest.raises(SystemExit) as exc_info:
        main.main()

    assert exc_info.value.code == 1
    captured = capsys.readouterr()
    assert "Error: GEMINI_API_KEY is required" in captured.err


@patch("main.Application")
def test_should_handle_extraction_failure_gracefully(mock_app_class, capsys):
    mock_app = MagicMock()
    mock_app.extract_tweets.return_value = ExtractionResult(
        success=False, error_type="archive_not_found", error_message="Archive not found"
    )
    mock_app_class.return_value = mock_app

    sys.argv = ["main.py", "extract-tweets"]

    with pytest.raises(SystemExit) as exc_info:
        main.main()

    assert exc_info.value.code == 1
    captured = capsys.readouterr()
    assert "Error: Archive not found" in captured.err


@patch("main.Application")
def test_should_handle_invalid_format_error_gracefully(mock_app_class, capsys):
    mock_app = MagicMock()
    mock_app.extract_tweets.return_value = ExtractionResult(
        success=False, error_type="invalid_format", error_message="Invalid JSON format"
    )
    mock_app_class.return_value = mock_app

    sys.argv = ["main.py", "extract-tweets"]

    with pytest.raises(SystemExit) as exc_info:
        main.main()

    assert exc_info.value.code == 1
    captured = capsys.readouterr()
    assert "Error: Invalid JSON format" in captured.err
