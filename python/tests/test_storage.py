import csv
import json
import os

import pytest

from models import AnalysisResult, Tweet
from storage import Checkpoint, CSVParser, CSVWriter, JSONParser


def test_should_parse_tweets_when_valid_json_file_provided(testdata_dir):
    parser = JSONParser(str(testdata_dir / "tweets.json"))
    tweets = parser.parse()

    assert len(tweets) == 4
    assert tweets[0].id == "1234567890123456789"
    assert "#golang error handling" in tweets[0].content
    assert tweets[1].id == "9876543210987654321"
    assert "@randomuser" in tweets[1].content


def test_should_raise_error_when_json_file_not_found(tmp_path):
    parser = JSONParser(str(tmp_path / "nonexistent.json"))

    with pytest.raises(FileNotFoundError):
        parser.parse()


def test_should_raise_error_when_json_file_invalid(testdata_dir):
    parser = JSONParser(str(testdata_dir / "invalid.json"))

    with pytest.raises(ValueError, match="Invalid JSON"):
        parser.parse()


def test_should_raise_error_when_json_missing_required_fields(tmp_path):
    incomplete_data = [{"tweet": {"id_str": "123"}}]  # Missing full_text
    file_path = tmp_path / "incomplete.json"
    with open(file_path, "w") as f:
        json.dump(incomplete_data, f)

    parser = JSONParser(str(file_path))

    with pytest.raises(ValueError, match="Missing required field"):
        parser.parse()


def test_should_parse_tweets_when_valid_csv_file_provided(testdata_dir):
    parser = CSVParser(str(testdata_dir / "tweets.csv"))
    tweets = parser.parse()

    assert len(tweets) == 2
    assert tweets[0].id == "1234567890123456789"
    assert "#golang error handling" in tweets[0].content
    assert tweets[1].id == "9876543210987654321"


def test_should_raise_error_when_csv_file_not_found(tmp_path):
    parser = CSVParser(str(tmp_path / "nonexistent.csv"))

    with pytest.raises(FileNotFoundError):
        parser.parse()


def test_should_raise_error_when_csv_missing_required_columns(tmp_path):
    invalid_csv = tmp_path / "invalid.csv"
    with open(invalid_csv, "w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["wrong", "headers"])
        writer.writerow(["123", "content"])

    parser = CSVParser(str(invalid_csv))

    with pytest.raises(ValueError, match="Missing required column"):
        parser.parse()


def test_should_handle_csv_with_special_characters(tmp_path):
    special_csv = tmp_path / "special.csv"
    with open(special_csv, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["id", "text"])
        writer.writerow(["123", 'Tweet with, comma and "quotes"'])
        writer.writerow(["456", "Tweet with\nnewline"])

    parser = CSVParser(str(special_csv))
    tweets = parser.parse()

    assert len(tweets) == 2
    assert tweets[0].content == 'Tweet with, comma and "quotes"'
    assert tweets[1].content == "Tweet with\nnewline"


def test_should_return_zero_when_checkpoint_file_empty(tmp_path):
    checkpoint_path = tmp_path / "checkpoint.txt"

    with Checkpoint(str(checkpoint_path)) as cp:
        value = cp.load()

    assert value == 0


def test_should_save_and_load_checkpoint_value(tmp_path):
    checkpoint_path = tmp_path / "checkpoint.txt"

    with Checkpoint(str(checkpoint_path)) as cp:
        cp.save(42)

    with Checkpoint(str(checkpoint_path)) as cp:
        value = cp.load()

    assert value == 42


def test_should_create_directory_when_checkpoint_path_nested(tmp_path):
    nested_path = tmp_path / "data" / "checkpoints" / "progress.txt"

    with Checkpoint(str(nested_path)) as cp:
        cp.save(100)

    assert nested_path.exists()
    assert nested_path.parent.exists()


def test_should_set_file_permissions_when_checkpoint_created(tmp_path):
    checkpoint_path = tmp_path / "checkpoint.txt"

    with Checkpoint(str(checkpoint_path)) as cp:
        cp.save(5)

    stat = os.stat(checkpoint_path)
    mode = stat.st_mode & 0o777
    assert mode == 0o600


def test_should_raise_error_when_checkpoint_content_invalid(tmp_path):
    checkpoint_path = tmp_path / "invalid.txt"
    checkpoint_path.write_text("not a number")

    with Checkpoint(str(checkpoint_path)) as cp:
        with pytest.raises(ValueError, match="Corrupted checkpoint"):
            cp.load()


def test_should_raise_error_when_checkpoint_used_outside_context():
    cp = Checkpoint("/tmp/test.txt")

    with pytest.raises(RuntimeError, match="not open"):
        cp.load()


def test_should_write_tweets_to_csv_file(tmp_path):
    output_path = tmp_path / "output.csv"
    tweets = [
        Tweet(id="123", content="First tweet"),
        Tweet(id="456", content="Second tweet"),
    ]

    with CSVWriter(str(output_path)) as writer:
        writer.write_tweets(tweets)

    with open(output_path, newline="") as f:
        reader = csv.reader(f)
        rows = list(reader)

    assert rows[0] == ["id", "text"]
    assert rows[1] == ["123", "First tweet"]
    assert rows[2] == ["456", "Second tweet"]


def test_should_append_tweets_when_append_mode_enabled(tmp_path):
    output_path = tmp_path / "append.csv"
    tweets = [
        Tweet(id="123", content="First tweet"),
        Tweet(id="456", content="Second tweet"),
    ]

    with CSVWriter(str(output_path)) as writer:
        writer.write_tweets([tweets[0]])

    with CSVWriter(str(output_path), append=True) as writer:
        writer.write_tweets([tweets[1]])

    with open(output_path, newline="") as f:
        reader = csv.reader(f)
        rows = list(reader)

    assert len(rows) == 3  # header + 2 data rows
    assert rows[0] == ["id", "text"]
    assert rows[1] == ["123", "First tweet"]
    assert rows[2] == ["456", "Second tweet"]


def test_should_write_analysis_results_to_csv(tmp_path):
    output_path = tmp_path / "results.csv"
    results = [
        AnalysisResult(tweet_url="https://twitter.com/u/s/1", should_delete=False),
        AnalysisResult(tweet_url="https://twitter.com/u/s/2", should_delete=True),
    ]

    with CSVWriter(str(output_path)) as writer:
        for result in results:
            writer.write_result(result)

    with open(output_path, newline="") as f:
        reader = csv.reader(f)
        rows = list(reader)

    assert rows[0] == ["tweet_url", "deleted"]
    assert rows[1] == ["https://twitter.com/u/s/1", "false"]
    assert rows[2] == ["https://twitter.com/u/s/2", "true"]


def test_should_create_directory_when_csv_path_nested(tmp_path):
    nested_path = tmp_path / "output" / "data" / "tweets.csv"

    with CSVWriter(str(nested_path)) as writer:
        writer.write_tweets([Tweet(id="1", content="test")])

    assert nested_path.exists()
    assert nested_path.parent.exists()


def test_should_set_file_permissions_when_csv_created(tmp_path):
    output_path = tmp_path / "secure.csv"

    with CSVWriter(str(output_path)) as writer:
        writer.write_tweets([Tweet(id="1", content="test")])

    stat = os.stat(output_path)
    mode = stat.st_mode & 0o777
    assert mode == 0o600


def test_should_raise_error_when_writer_used_outside_context(tmp_path):
    writer = CSVWriter(str(tmp_path / "test.csv"))

    with pytest.raises(RuntimeError, match="not open"):
        writer.write_tweets([])
