import json
import logging
import os
import sys
from dataclasses import dataclass, field
from functools import lru_cache
from pathlib import Path

from dotenv import load_dotenv

load_dotenv()


def configure_logging() -> None:
    log_level = os.getenv("LOG_LEVEL", "INFO").upper()
    log_format = "%(asctime)s - %(name)s - %(levelname)s - %(message)s"

    logging.basicConfig(
        level=getattr(logging, log_level, logging.INFO),
        format=log_format,
        handlers=[logging.StreamHandler(sys.stdout)],
    )


@dataclass
class Criteria:
    additional_instructions: str = ""
    forbidden_words: list[str] = field(default_factory=list)
    topics_to_exclude: list[str] = field(default_factory=list)
    tone_requirements: list[str] = field(default_factory=list)


@dataclass
class Settings:
    tweets_archive_path: str = "data/tweets/tweets.json"
    transformed_tweets_path: str = "data/tweets/transformed/tweets.csv"
    checkpoint_path: str = "data/checkpoint.txt"
    processed_results_path: str = "data/tweets/processed/results.csv"
    base_twitter_url: str = "https://x.com"
    x_username: str = "Benn_X1"
    gemini_api_key: str = ""
    gemini_model: str = "gemini-2.5-flash"
    batch_size: int = 10
    rate_limit_seconds: float = 1.0
    criteria: Criteria = field(default_factory=Criteria)

    def tweet_url(self, tweet_id: str) -> str:
        return f"{self.base_twitter_url}/{self.x_username}/status/{tweet_id}"


def _default_criteria() -> Criteria:
    return Criteria(
        forbidden_words=[],
        topics_to_exclude=[
            "Profanity or unprofessional language",
            "Personal attacks or insults",
            "Outdated political opinions",
        ],
        tone_requirements=[
            "Professional language only",
            "Respectful communication",
        ],
        additional_instructions="Flag any content that could harm professional reputation",
    )


def _load_config_file(path: str) -> Criteria | None:
    try:
        config_path = Path(path)
        if not config_path.exists():
            return None

        with open(config_path, encoding="utf-8") as f:
            data = json.load(f)

        criteria_data = data.get("criteria", {})
        return Criteria(
            additional_instructions=criteria_data.get("additional_instructions", ""),
            forbidden_words=criteria_data.get("forbidden_words", []),
            topics_to_exclude=criteria_data.get("topics_to_exclude", []),
            tone_requirements=criteria_data.get("tone_requirements", []),
        )
    except (OSError, json.JSONDecodeError):
        return None


@lru_cache(maxsize=1)
def load_settings() -> Settings:
    configure_logging()

    criteria = _default_criteria()

    if file_criteria := _load_config_file("config.json"):
        criteria = file_criteria

    return Settings(
        x_username=os.getenv("X_USERNAME", "Benn_X1"),
        gemini_api_key=os.getenv("GEMINI_API_KEY", ""),
        gemini_model=os.getenv("GEMINI_MODEL", "gemini-2.5-flash"),
        rate_limit_seconds=float(os.getenv("RATE_LIMIT_SECONDS", "1.0")),
        criteria=criteria,
    )


settings = load_settings()
