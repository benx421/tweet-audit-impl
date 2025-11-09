# Technical Tradeoffs & Design Decisions

## Language Choice: Java

I chose Java for its strong compile-time type safety and mature ecosystem. The static type system catches errors before runtime, and the JVM provides excellent performance and memory management for batch processing workloads. Modern Java features (records, text blocks, switch expressions) reduce boilerplate significantly.

The downside is verbosity. Even with modern features, Java requires more code for the same functionality due to explicit error handling, type declarations, and object construction boilerplate. Deployment requires a JVM runtime, though the fat JAR approach bundles everything into a single file.

## Architecture: Batch Processing with Checkpointing

I designed the tool to process tweets in fixed-size batches, save progress after each batch, and exit. This produces short-lived processes suitable for scheduled execution via cron or manual re-runs.

Sequential processing trades speed for simplicity. It naturally rate-limits API calls, makes checkpoint logic trivial (single integer), and ensures failures don't lose progress. Concurrent processing would be faster but adds complexity around partial failures, coordinated retries, and state synchronization.

## Abstraction Layers: Interfaces for Testability

I defined interfaces (`TweetAnalyzer`, `GeminiClient`) even though only one implementation exists for each. This enables testing with mock implementations without requiring a mocking framework.

The tradeoff is added abstraction. For a small tool, this might be over-engineering, but it demonstrates production patterns: testability through dependency injection and separation of contract from implementation.

## Resilience: Retry and Rate Limiting

I included both exponential backoff retry (for transient API errors) and rate limiting (to prevent quota exhaustion) in the analyzer. This handles API flakiness automatically while preventing quota violations.

The application layer doesn't retry. When a batch fails, the checkpoint doesn't update, so rerunning the tool retries that batch naturally. This keeps orchestration simple while handling both transient errors and persistent failures.

## Error Handling Philosophy

I chose fail-fast error handling. Errors propagate up the call stack wrapped with context. When analysis fails, the application immediately stops processing and reports the error. The checkpoint doesn't update, so rerunning retries from that point.

This contrasts with resilient systems that skip failed items and continue. For this workflow, stopping on errors is safer: issues surface immediately rather than silently skipping tweets. Users can investigate and fix problems (API key, network, rate limits) before resuming.

## Output Format: Deletion Candidates Only

I write only deletion candidates to the results CSV, not all analyzed tweets. Each row includes the tweet URL and a `deleted` column initialized to `false`.

This creates a focused action list while minimizing file size. The tradeoff is losing a complete audit trail. If decisions need review later, the tool must be re-run. For the manual cleanup workflow, the focused approach is more practical.
