# Tweet Audit

This repository contains the implementation of the [Tweet Audit Project](https://github.com/benx421/tweet-audit), which evaluates tweets against predetermined alignment criteria (e.g., unprofessional language, specific keywords, outdated opinions), and generates a list of tweet URLs marked for manual deletion.

The implementation is done in 3 languages: Go, Python and Java. Each version follows similar logic and structure, serving as a reference for developers who want to complete the same task in their preferred programming language.

## Project Structure

Each implementation is located in its respective directory:

```text
tweet-audit/
├── go/          # Go implementation
├── python/      # Python implementation
└── java/        # Java implementation
```

Refer to the README in each language directory for detailed installation, configuration, and usage instructions.

## Development Standards

All implementations follow these standards:

- Mandatory unit tests for core logic
- Error handling and logging
- Configuration-based criteria evaluation
- Modular, maintainable code structure
- **TRADEOFFS.md**: Documentation of architectural decisions and trade-offs
