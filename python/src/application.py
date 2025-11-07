import logging

from analyzer import Gemini
from config import settings
from models import Decision, Result
from storage import Checkpoint, CSVParser, CSVWriter, JSONParser

logger = logging.getLogger(__name__)


class Application:
    def __init__(self):
        self._analyzer = None

    @staticmethod
    def _build_error_result(e: Exception, context: str = "") -> Result:
        error_msg = str(e)

        if isinstance(e, FileNotFoundError):
            logger.error(f"Required file not found{f' during {context}' if context else ''}: {e}")
            return Result(success=False, error_type="file_not_found", error_message=error_msg)

        if isinstance(e, ValueError):
            logger.error(f"Invalid data format{f' during {context}' if context else ''}: {e}")
            return Result(success=False, error_type="invalid_format", error_message=error_msg)

        if isinstance(e, PermissionError):
            logger.error(f"Permission denied{f' during {context}' if context else ''}: {e}")
            return Result(success=False, error_type="permission_denied", error_message=error_msg)

        logger.error(
            f"Unexpected error{f' during {context}' if context else ''}: {e}", exc_info=True
        )
        return Result(
            success=False,
            error_type="unexpected_error",
            error_message="An unexpected error occurred.",
        )

    @property
    def analyzer(self) -> Gemini:
        if self._analyzer is None:
            self._analyzer = Gemini()
            logger.info("Gemini analyzer initialized")
        return self._analyzer

    def extract_tweets(self) -> Result:
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
            return Result(success=True, count=len(tweets))
        except Exception as e:
            return self._build_error_result(e, context="extraction")

    def analyze_tweets(self) -> Result:
        try:
            logger.info(f"Loading tweets from {settings.transformed_tweets_path}")
            parser = CSVParser(settings.transformed_tweets_path)
            tweets = parser.parse()

            if not tweets:
                logger.warning("No tweets found to analyze")
                return Result(success=True, count=0)

            logger.info(f"Loaded {len(tweets)} tweets for analysis")

            analyzed_count = 0
            with Checkpoint(settings.checkpoint_path) as checkpoint:
                start_index = checkpoint.load()
                logger.info(f"Resuming from tweet index {start_index}")

                with CSVWriter(settings.processed_results_path, append=True) as writer:
                    for i in range(start_index, len(tweets), settings.batch_size):
                        batch = tweets[i : i + settings.batch_size]
                        batch_num = (i // settings.batch_size) + 1
                        total_batches = (
                            len(tweets) + settings.batch_size - 1
                        ) // settings.batch_size

                        logger.info(
                            f"Processing batch {batch_num}/{total_batches} "
                            f"(tweets {i+1}-{min(i+len(batch), len(tweets))} of {len(tweets)})"
                        )

                        for tweet in batch:
                            try:
                                result = self.analyzer.analyze(tweet)
                                logger.debug(f"Tweet {tweet.id}: {result.decision.value}")
                                analyzed_count += 1

                                if result.decision == Decision.DELETE:
                                    writer.write_result(result)
                            except Exception as e:
                                logger.error(
                                    f"Failed to analyze tweet {tweet.id}: {e}", exc_info=True
                                )
                                return Result(
                                    success=False,
                                    count=analyzed_count,
                                    error_type="analysis_failed",
                                    error_message=str(e),
                                )

                        checkpoint.save(i + len(batch))
                        logger.info(f"Checkpoint saved at index {i + len(batch)}")

            logger.info(f"Analysis complete. Results written to {settings.processed_results_path}")
            return Result(success=True, count=analyzed_count)
        except Exception as e:
            return self._build_error_result(e, context="analysis")
