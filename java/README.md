# Tweet Audit | Java Implementation

Java implementation of the Tweet Audit tool.

## Prerequisites

- Java 25
- Maven 3.9+

## Build & Run

```bash
# Build
mvn clean package

# Run
java -jar target/tweet-audit.jar
```

## Development

```bash
# Run tests
mvn test

# Format code
mvn spotless:apply

# Check formatting
mvn spotless:check

# Check code style
mvn checkstyle:check

# Full build with all checks
mvn clean verify
```

## License

See the root [LICENSE](../LICENSE) file.
