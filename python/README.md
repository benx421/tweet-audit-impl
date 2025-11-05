# Tweet Audit | Python Implementation

This is the Python implementation of the Tweet Audit tool, which evaluates tweets against predetermined alignment criteria and generates a list of tweet URLs marked for manual deletion.

## Prerequisites

- Python 3.11 or higher
- [Poetry](https://python-poetry.org/) for dependency management

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

## Usage

### Run the CLI

```bash
python src/main.py
python src/main.py extract-tweets
python src/main.py analyze-tweets
```

## Development

### Running Tests

```bash
# Run all tests
poetry run pytest

# Run with coverage
poetry run pytest --cov

# Run with verbose output
poetry run pytest -v
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
- pytest for testing with coverage
- Poetry 2.0 for dependency management

### CI/CD

GitHub Actions (`.github/workflows/python.yml`) runs on every push/PR that touches `python/**`:

- Tests on Python 3.11
- Linting and formatting via ruff
- Coverage reporting

## License

See the root [LICENSE](../LICENSE) file for details.
