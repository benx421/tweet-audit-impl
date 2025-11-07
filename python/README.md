# Tweet Audit | Python Implementation

This is the Python implementation of the Tweet Audit tool, which evaluates tweets against predetermined alignment criteria and generates a list of tweet URLs marked for manual deletion.

## Prerequisites

- Python 3.11 or higher
- [Poetry](https://python-poetry.org/) for dependency management
- Google Gemini API key ([get one here](https://ai.google.dev/))

## Installation

### Install Poetry

```bash
# macOS/Linux
curl -sSL https://install.python-poetry.org | python3 -

# Or via pip
pip install poetry
```

### Clone and Setup

```bash
git clone https://github.com/benx421/tweet-audit.git
cd tweet-audit/python
poetry install
```

## Configuration

### Required: API Key

Set your Gemini API key as an environment variable:

```bash
export GEMINI_API_KEY="your-api-key-here"
```

### Optional: Custom Criteria

Create a `config.json` file (see `config.example.json`):

```json
{
  "criteria": {
    "forbidden_words": ["profanity1", "profanity2"],
    "topics_to_exclude": ["Outdated opinions", "Controversial statements"],
    "tone_requirements": ["Professional language only"],
    "additional_instructions": "Flag any content that could harm professional reputation"
  }
}
```

If not provided, default criteria will be used.

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `GEMINI_API_KEY` | *(required)* | Your Google Gemini API key |
| `TWITTER_USERNAME` | `"user"` | Your Twitter username for URL generation |
| `TWEETS_ARCHIVE_PATH` | `"data/tweets/tweets.json"` | Path to Twitter archive JSON |
| `TRANSFORMED_TWEETS_PATH` | `"data/tweets/transformed/tweets.csv"` | Output path for extracted tweets |
| `PROCESSED_RESULTS_PATH` | `"data/processed/results.csv"` | Output path for analysis results |
| `CHECKPOINT_PATH` | `"data/processed/checkpoint.txt"` | Checkpoint file for resuming |
| `CONFIG_FILE_PATH` | `"config.json"` | Path to criteria config file |
| `BATCH_SIZE` | `10` | Number of tweets to process per batch |
| `RATE_LIMIT_DELAY` | `1.0` | Delay between API calls (seconds) |
| `LOG_LEVEL` | `"INFO"` | Logging level (DEBUG, INFO, WARNING, ERROR) |

## Usage

### Extract Tweets

Download your Twitter archive and place `tweets.json` in `data/tweets/`:

```bash
poetry run python src/main.py extract-tweets
```

This creates `data/tweets/transformed/tweets.csv`.

### Analyze Tweets

```bash
poetry run python src/main.py analyze-tweets
```

This processes all tweets in batches, saving progress to a checkpoint after each batch. If interrupted, re-run the command to resume from the checkpoint.

Only tweets marked for deletion are written to `data/processed/results.csv` with columns: `tweet_url`, `deleted` (initially false, for manual tracking).

## Development

### Running Tests

```bash
# Run all tests
poetry run pytest

# Run with coverage
poetry run pytest --cov

# Run specific test file
poetry run pytest tests/test_application.py -v
```

### Linting and Formatting

```bash
# Run ruff linter
poetry run ruff check .

# Auto-fix linting issues
poetry run ruff check --fix .

# Format code
poetry run ruff format .

# Check formatting without changes
poetry run ruff format --check .
```

### Code Standards

This implementation follows:

- Python 3.11+ with type hints
- Ruff for linting and formatting
- pytest for testing with 96% coverage
- Poetry 2.0 for dependency management

### CI/CD

GitHub Actions (`.github/workflows/python.yml`) runs on every push/PR that touches `python/**`:

- Tests on Python 3.11
- Linting and formatting via ruff
- Coverage reporting

## License

See the root [LICENSE](../LICENSE) file for details.
