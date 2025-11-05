import sys

import main


def test_should_show_help_when_no_args_provided(capsys):
    sys.argv = ["main.py"]
    main.main()
    captured = capsys.readouterr()
    assert "usage:" in captured.out


def test_should_extract_tweets_when_extract_command_given(capsys):
    sys.argv = ["main.py", "extract-tweets"]
    main.main()
    captured = capsys.readouterr()
    assert "Extracting tweets from archive..." in captured.out


def test_should_analyze_tweets_when_analyze_command_given(capsys):
    sys.argv = ["main.py", "analyze-tweets"]
    main.main()
    captured = capsys.readouterr()
    assert "Analyzing tweets..." in captured.out
