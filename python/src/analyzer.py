import json
import time
from functools import wraps

import google.generativeai as genai

from config import settings
from models import AnalysisResult, Decision, Tweet


def retry_with_backoff(max_retries: int = 3, initial_delay: float = 1.0):
    """Retry decorator with exponential backoff for transient errors"""

    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            delay = initial_delay
            last_exception = None

            for attempt in range(max_retries):
                try:
                    return func(*args, **kwargs)
                except Exception as e:
                    last_exception = e
                    error_str = str(e).lower()
                    is_retryable = any(
                        keyword in error_str
                        for keyword in [
                            "timeout",
                            "connection",
                            "rate limit",
                            "quota",
                            "503",
                            "429",
                            "temporarily unavailable",
                        ]
                    )

                    if not is_retryable or attempt == max_retries - 1:
                        raise

                    sleep_time = delay * (2**attempt) + (time.time() % 1)
                    time.sleep(sleep_time)

            raise last_exception

        return wrapper

    return decorator


class Gemini:
    def __init__(self):
        if not settings.gemini_api_key:
            raise ValueError(
                "GEMINI_API_KEY is required. Set it via environment variable or .env file"
            )

        genai.configure(api_key=settings.gemini_api_key)
        self.model = genai.GenerativeModel(settings.gemini_model)
        self.last_request_time = 0
        self.min_request_interval = settings.rate_limit_seconds

    def _rate_limit(self) -> None:
        elapsed = time.time() - self.last_request_time
        if elapsed < self.min_request_interval:
            time.sleep(self.min_request_interval - elapsed)
        self.last_request_time = time.time()

    @retry_with_backoff(max_retries=3, initial_delay=1.0)
    def analyze(self, tweet: Tweet) -> AnalysisResult:
        self._rate_limit()  # Enforce rate limiting before API call

        prompt = self._build_prompt(tweet)

        response = self.model.generate_content(
            prompt,
            generation_config=genai.GenerationConfig(response_mime_type="application/json"),
        )

        if not response.text:
            raise ValueError(f"Empty response from Gemini for tweet {tweet.id}")

        try:
            data = json.loads(response.text)
        except json.JSONDecodeError as e:
            raise ValueError(
                f"Invalid Gemini response for tweet {tweet.id}: {e} (response: {response.text})"
            ) from e

        try:
            decision = Decision(data["decision"].upper())
        except KeyError as e:
            raise ValueError(
                f"Missing decision field in Gemini response for tweet {tweet.id}: {e} "
                f"(response: {response.text})"
            ) from e
        except ValueError as e:
            raise ValueError(
                f"Invalid decision value from Gemini for tweet {tweet.id}: {e} "
                f"(response: {response.text})"
            ) from e

        return AnalysisResult(tweet_url=settings.tweet_url(tweet.id), decision=decision)

    def _build_prompt(self, tweet: Tweet) -> str:
        criteria_parts = []

        criteria_parts.extend(settings.criteria.topics_to_exclude)
        criteria_parts.extend(settings.criteria.tone_requirements)

        if settings.criteria.forbidden_words:
            words = ", ".join(settings.criteria.forbidden_words)
            criteria_parts.append(f"Contains any of these words: {words}")

        criteria_list = "\n".join(f"{i+1}. {c}" for i, c in enumerate(criteria_parts))

        additional = ""
        if settings.criteria.additional_instructions:
            additional = f"\n\nAdditional guidance: {settings.criteria.additional_instructions}"

        return f"""You are evaluating tweets for a professional's Twitter cleanup.

Tweet ID: {tweet.id}
Tweet: "{tweet.content}"

Mark for deletion if it violates any of these criteria:
{criteria_list}{additional}

Respond in JSON format:
{{
  "decision": "DELETE" or "KEEP",
  "reason": "brief explanation"
}}"""
