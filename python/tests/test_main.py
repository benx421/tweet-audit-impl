import sys
from unittest.mock import MagicMock, patch

import pytest

import main
from models import Result


def test_should_show_help_when_no_args_provided(capsys):
    sys.argv = ["main.py"]
    main.main()
    captured = capsys.readouterr()
    assert "usage:" in captured.out


@patch("main.Application")
def test_should_extract_tweets_when_extract_command_given(mock_app_class, capsys):
    mock_app = MagicMock()
    mock_app.extract_tweets.return_value = Result(success=True, count=42)
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
    mock_app.analyze_tweets.return_value = Result(success=True, count=100)
    mock_app_class.return_value = mock_app

    sys.argv = ["main.py", "analyze-tweets"]
    main.main()

    captured = capsys.readouterr()
    assert "Analyzing tweets..." in captured.out
    assert "Successfully analyzed 100 tweets" in captured.out
    mock_app.analyze_tweets.assert_called_once()


@pytest.mark.parametrize(
    "error_type,error_message",
    [
        ("analysis_failed", "API error occurred"),
        ("file_not_found", "CSV file not found"),
        ("invalid_format", "Missing required column"),
    ],
)
@patch("main.Application")
def test_should_handle_analysis_errors_gracefully(
    mock_app_class, capsys, error_type, error_message
):
    mock_app = MagicMock()
    mock_app.analyze_tweets.return_value = Result(
        success=False, error_type=error_type, error_message=error_message
    )
    mock_app_class.return_value = mock_app

    sys.argv = ["main.py", "analyze-tweets"]

    with pytest.raises(SystemExit) as exc_info:
        main.main()

    assert exc_info.value.code == 1
    captured = capsys.readouterr()
    assert f"Error: {error_message}" in captured.err


@pytest.mark.parametrize(
    "error_type,error_message",
    [
        ("file_not_found", "Archive not found"),
        ("invalid_format", "Invalid JSON format"),
        ("permission_denied", "Permission denied"),
    ],
)
@patch("main.Application")
def test_should_handle_extraction_errors_gracefully(
    mock_app_class, capsys, error_type, error_message
):
    mock_app = MagicMock()
    mock_app.extract_tweets.return_value = Result(
        success=False, error_type=error_type, error_message=error_message
    )
    mock_app_class.return_value = mock_app

    sys.argv = ["main.py", "extract-tweets"]

    with pytest.raises(SystemExit) as exc_info:
        main.main()

    assert exc_info.value.code == 1
    captured = capsys.readouterr()
    assert f"Error: {error_message}" in captured.err
