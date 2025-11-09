# Tweet Audit | Java Implementation

This is the Java implementation of the Tweet Audit tool, which evaluates tweets against predetermined alignment criteria and generates a list of tweet URLs marked for manual deletion.

## How It Works

The tool works in two stages:

1. **Extract**: Parse your Twitter archive JSON and convert tweets to CSV
2. **Analyze**: Run tweets through Gemini AI in batches, flag problematic ones, save progress

The analyze command processes tweets in configurable batches (default: 10), saves a checkpoint after each batch, and can be run multiple times to work through large archives incrementally.

## Prerequisites

- Java 25
- Maven 3.9+
- Google Gemini API key ([get one here](https://ai.google.dev/))

## Installation

Download and install Java 25 from [Oracle](https://www.oracle.com/java/technologies/downloads/) or use [SDKMAN](https://sdkman.io/):

```bash
# Using SDKMAN
sdk install java 25-open

# Clone and build
git clone https://github.com/benx421/tweet-audit.git
cd tweet-audit/java
mvn clean package
```

## Configuration

### Environment Variables

```bash
export GEMINI_API_KEY="your-api-key-here"
export X_USERNAME="your_twitter_handle"      # Default: user
export GEMINI_MODEL="gemini-2.5-flash"       # Default: gemini-2.5-flash
export BATCH_SIZE="10"                       # Default: 10
export RATE_LIMIT_SECONDS="1.0"             # Default: 1.0
```

### Criteria Configuration

Create a `config.json` file to define what tweets should be flagged (see `config.example.json`):

```json
{
  "criteria": {
    "forbidden_words": ["crypto", "NFT"],
    "topics_to_exclude": [
      "Profanity or unprofessional language",
      "Personal attacks or insults",
      "Outdated political opinions"
    ],
    "tone_requirements": [
      "Professional language only",
      "Respectful communication"
    ],
    "additional_instructions": "Flag any content that could harm professional reputation"
  }
}
```

If you don't provide a config file, the tool uses sensible defaults focused on professional reputation.

## Usage

### Extract Tweets from Archive

```bash
# Place your Twitter archive at data/tweets/tweets.json
java -jar target/tweet-audit.jar extract-tweets
```

This parses the archive and creates `data/tweets/transformed/tweets.csv`.

### Analyze Tweets

```bash
# Process one batch (picks up where it left off)
java -jar target/tweet-audit.jar analyze-tweets
```

What happens:
- Processes tweets in batches (default: 10 per batch)
- Saves progress to `data/checkpoint.txt`
- Writes flagged tweets to `data/tweets/processed/results.csv`
- Run multiple times to work through your entire archive

The output CSV has two columns:
- `tweet_url`: Full URL to the flagged tweet
- `deleted`: Status flag (starts as `false`, you update it manually after deleting)

### Workflow Example

```bash
# Step 1: Extract tweets from archive
java -jar target/tweet-audit.jar extract-tweets

# Step 2: Run analysis in batches
java -jar target/tweet-audit.jar analyze-tweets  # Process tweets 0-9
java -jar target/tweet-audit.jar analyze-tweets  # Process tweets 10-19
java -jar target/tweet-audit.jar analyze-tweets  # Process tweets 20-29
# ... continue until all tweets processed
```

## Development

### Running Tests

```bash
# Run all tests
mvn test
```

### Code Quality

```bash
# Format code
mvn spotless:apply

# Check formatting
mvn spotless:check

# Check code style
mvn checkstyle:check

# Full build with all checks
mvn clean verify
```

### Code Standards

This implementation follows:

- Java 25 with modern language features (records, text blocks, switch expressions)
- Builder pattern for complex object construction
- Interface-based design for testability
- Comprehensive error handling and logging
- 2-space indentation (enforced by Spotless)
- Checkstyle for code quality

### CI/CD

GitHub Actions (`.github/workflows/java.yml`) runs on every push/PR that touches `java/**`:

- Tests on Java 25
- Code formatting checks via Spotless
- Code style checks via Checkstyle
- JAR build verification

## Architecture

The implementation uses a clean layered architecture:

- **models**: Immutable data classes (Tweet, AnalysisResult, Decision)
- **config**: Configuration loading and settings management
- **storage**: File I/O operations (parsing, writing, checkpointing)
- **analyzer**: Gemini AI integration with retry and rate limiting
- **application**: Orchestration layer coordinating all components

See [TRADEOFFS.md](TRADEOFFS.md) for detailed architectural decisions and design rationale.

## License

See the root [LICENSE](../LICENSE) file for details.
