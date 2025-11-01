# Tweet Audit | Go Implementation

This is the Go implementation of the Tweet Audit tool, which evaluates tweets against predetermined alignment criteria and generates a list of tweet URLs marked for manual deletion.

## Prerequisites

- Go 1.25 or higher
- [golangci-lint](https://golangci-lint.run/) (for linting)

## Installation

### Install Go

Download and install Go from [go.dev/dl](https://go.dev/dl/)

### Install golangci-lint

```bash
# macOS
brew install golangci-lint

# Linux/macOS/Windows
go install github.com/golangci/golangci-lint/cmd/golangci-lint@latest

# Or see: https://golangci-lint.run/welcome/install/
```

### Clone and Setup

```bash
git clone https://github.com/benx421/tweet-audit.git
cd tweet-audit/go
go mod download
```

## Usage

### Build

```bash
# Build the binary
go build -o bin/tweet-audit ./cmd/tweet-audit

# Run the binary
./bin/tweet-audit
```

### Run without building

```bash
go run ./cmd/tweet-audit
```

## Development

### Running Tests

```bash
# Run all tests
go test ./...

# Run tests with coverage
go test -v -race -coverprofile=coverage.out ./...

# View coverage report
go tool cover -html=coverage.out
```

### Linting

```bash
# Run linter
golangci-lint run

# Run linter with auto-fix where possible
golangci-lint run --fix
```

### Code Standards

This implementation follows:

- **Standard Go project layout** ([golang-standards/project-layout](https://github.com/golang-standards/project-layout))
- **Comprehensive linting** with golangci-lint v2
  - Security checks (gosec)
  - Error handling enforcement (errcheck, errorlint)
  - Style consistency (gocritic, revive)
  - Code quality (staticcheck, govet)
- **Proper error handling** in all code, including tests
- **Test coverage** for core functionality
- **Idiomatic Go** patterns and conventions

### CI/CD

The GitHub Actions workflow (`.github/workflows/go.yml`) automatically:

- Runs tests on Go 1.24 and 1.25
- Executes golangci-lint
- Builds the binary
- Runs on every push/PR that modifies `go/**`

## License

See the root [LICENSE](../LICENSE) file for details.
