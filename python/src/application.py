import logging

from analyzer import Gemini
from config import settings
from models import ExtractionResult
from storage import CSVWriter, JSONParser

logger = logging.getLogger(__name__)


class Application:
    def __init__(self):
        self._analyzer = None

    @property
    def analyzer(self) -> Gemini:
        if self._analyzer is None:
            self._analyzer = Gemini()
            logger.info("Gemini analyzer initialized")
        return self._analyzer

    def extract_tweets(self) -> ExtractionResult:
        try:
            logger.info(f"Reading tweets from {settings.tweets_archive_path}")
            parser = JSONParser(settings.tweets_archive_path)
            tweets = parser.parse()

            logger.info(f"Extracted {len(tweets)} tweets, writing to CSV")
            with CSVWriter(settings.transformed_tweets_path) as writer:
                writer.write_tweets(tweets)

            logger.info(
                f"Successfully wrote {len(tweets)} tweets to {settings.transformed_tweets_path}"
            )
            return ExtractionResult(success=True, count=len(tweets))
        except FileNotFoundError as e:
            logger.error(f"Archive file not found: {e}")
            return ExtractionResult(
                success=False, error_type="archive_not_found", error_message=str(e)
            )
        except ValueError as e:
            logger.error(f"Invalid archive format: {e}")
            return ExtractionResult(
                success=False, error_type="invalid_format", error_message=str(e)
            )
        except PermissionError as e:
            logger.error(f"Permission denied: {e}")
            return ExtractionResult(
                success=False, error_type="permission_denied", error_message=str(e)
            )
        except Exception as e:
            logger.error(f"Unexpected error during extraction: {e}", exc_info=True)
            return ExtractionResult(
                success=False,
                error_type="unexpected_error",
                error_message="An unexpected error occurred.",
            )

    def analyze_tweets(self) -> None:
        pass
