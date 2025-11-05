import csv
import json
import os
from abc import ABC, abstractmethod

from models import AnalysisResult, Tweet

PRIVATE_FILE_MODE = 0o600  # Owner read/write only
PRIVATE_DIR_MODE = 0o750  # Owner read/write/execute, group read/execute

FILE_ENCODING = "utf-8"

# Twitter archive JSON fields (input format from Twitter export)
TWITTER_ARCHIVE_ID_FIELD = "id_str"
TWITTER_ARCHIVE_TEXT_FIELD = "full_text"

# Tweet CSV format (intermediate format for processing)
TWEET_CSV_ID_COLUMN = "id"
TWEET_CSV_TEXT_COLUMN = "text"

# Analysis result CSV format (output format)
RESULT_CSV_URL_COLUMN = "tweet_url"
RESULT_CSV_DELETED_COLUMN = "deleted"

CSV_BOOL_TRUE = "true"
CSV_BOOL_FALSE = "false"


class Parser(ABC):
    @abstractmethod
    def parse(self) -> list[Tweet]:
        raise NotImplementedError


class JSONParser(Parser):
    def __init__(self, file_path: str) -> None:
        self.file_path = file_path

    def parse(self) -> list[Tweet]:
        try:
            with open(self.file_path, encoding=FILE_ENCODING) as file:
                data = json.load(file)
                return [
                    Tweet(
                        id=item[TWITTER_ARCHIVE_ID_FIELD], content=item[TWITTER_ARCHIVE_TEXT_FIELD]
                    )
                    for item in data
                ]
        except FileNotFoundError as e:
            raise FileNotFoundError(f"Tweet archive not found: {self.file_path}") from e
        except json.JSONDecodeError as e:
            raise ValueError(f"Invalid JSON in {self.file_path}: {e}") from e
        except KeyError as e:
            raise ValueError(
                f"Missing required field {e} in {self.file_path}. "
                f"Expected fields: {TWITTER_ARCHIVE_ID_FIELD}, {TWITTER_ARCHIVE_TEXT_FIELD}"
            ) from e


class CSVParser(Parser):
    def __init__(self, file_path: str) -> None:
        self.file_path = file_path

    def parse(self) -> list[Tweet]:
        try:
            with open(self.file_path, encoding=FILE_ENCODING) as file:
                reader = csv.DictReader(file)
                return [
                    Tweet(id=row[TWEET_CSV_ID_COLUMN], content=row[TWEET_CSV_TEXT_COLUMN])
                    for row in reader
                ]
        except FileNotFoundError as e:
            raise FileNotFoundError(f"CSV file not found: {self.file_path}") from e
        except KeyError as e:
            raise ValueError(
                f"Missing required column {e} in {self.file_path}. "
                f"Expected columns: {TWEET_CSV_ID_COLUMN}, {TWEET_CSV_TEXT_COLUMN}"
            ) from e
        except csv.Error as e:
            raise ValueError(f"Invalid CSV format in {self.file_path}: {e}") from e


class Checkpoint:
    def __init__(self, file_path: str) -> None:
        self.path = file_path
        self.file = None

    def __enter__(self) -> "Checkpoint":
        dir_path = os.path.dirname(self.path)
        if dir_path:
            os.makedirs(dir_path, mode=PRIVATE_DIR_MODE, exist_ok=True)

        self.file = open(self.path, "a+", encoding=FILE_ENCODING)
        os.chmod(self.path, PRIVATE_FILE_MODE)
        return self

    def __exit__(self, exc_type, exc_value, traceback) -> bool:
        if self.file:
            self.file.close()
            self.file = None
        return False

    def load(self) -> int:
        if not self.file:
            raise RuntimeError("Checkpoint file is not open")

        self.file.seek(0)
        content = self.file.read().strip()

        if not content:
            return 0

        try:
            return int(content)
        except ValueError as e:
            raise ValueError(
                f"Corrupted checkpoint file {self.path}: expected integer, got '{content}'"
            ) from e

    def save(self, tweet_index: int) -> None:
        if not self.file:
            raise RuntimeError("Checkpoint file is not open")

        self.file.seek(0)
        self.file.truncate()
        self.file.write(str(tweet_index))
        self.file.flush()


class CSVWriter:
    def __init__(self, file_path: str, append: bool = False) -> None:
        self.file_path = file_path
        self.append = append
        self.file = None
        self.writer = None
        self.header_written = False

    def __enter__(self) -> "CSVWriter":
        dir_path = os.path.dirname(self.file_path)
        if dir_path:
            os.makedirs(dir_path, mode=PRIVATE_DIR_MODE, exist_ok=True)

        file_exists = os.path.exists(self.file_path)
        self.header_written = self.append and file_exists

        mode = "a" if self.append and file_exists else "w"
        self.file = open(self.file_path, mode, encoding=FILE_ENCODING, newline="")
        self.writer = csv.writer(self.file)

        os.chmod(self.file_path, PRIVATE_FILE_MODE)
        return self

    def __exit__(self, exc_type, exc_value, traceback) -> bool:
        if self.file:
            self.file.close()
            self.file = None
            self.writer = None
        return False

    def write_tweets(self, tweets: list[Tweet]) -> None:
        if not self.writer:
            raise RuntimeError("CSVWriter is not open")

        if not self.header_written:
            self.writer.writerow([TWEET_CSV_ID_COLUMN, TWEET_CSV_TEXT_COLUMN])
            self.header_written = True

        for tweet in tweets:
            self.writer.writerow([tweet.id, tweet.content])

    def write_result(self, result: AnalysisResult) -> None:
        if not self.writer:
            raise RuntimeError("CSVWriter is not open")

        if not self.header_written:
            self.writer.writerow([RESULT_CSV_URL_COLUMN, RESULT_CSV_DELETED_COLUMN])
            self.header_written = True

        deleted = CSV_BOOL_TRUE if result.should_delete else CSV_BOOL_FALSE
        self.writer.writerow([result.tweet_url, deleted])
        self.file.flush()
