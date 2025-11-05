# Technical Tradeoffs & Design Decisions

## Language Choice: Go

Go fits this project well. Type safety catches bugs at compile time, it ships as a single binary (no dependency management), and the standard library handles CSV/JSON parsing natively. It's also fast and memory-efficient for batch processing large datasets.

The downside is slower prototyping compared to Python or Node.

## Architecture: Batch Processing with Checkpointing

The tool processes tweets in fixed-size batches, saves progress after each batch, and exits. This design is cron-friendly: each run is short (60-90 seconds), easy to monitor, and stateless.

Sequential processing trades speed for simplicity. It naturally rate-limits API calls, makes debugging straightforward (predictable execution flow), and the checkpoint mechanism means failures don't lose progress. The next run just picks up where it left off.

I could've used concurrent processing for 5-10x faster throughput, but that adds complexity: partial batch failures, retry logic, rate limit management, and harder-to-track progress. For a workflow that runs continuously on a schedule, the sequential approach is more maintainable.

## Modular Package Structure

The codebase separates concerns into distinct layers: domain models, I/O operations, external service integration, and orchestration. This makes testing easier because each component can be tested independently, localizes changes when requirements change, and allows infrastructure code to be reused elsewhere.

The tradeoff is more files and packages instead of a flat structure.

## Simple State Management

Progress tracking stores a single integer: the index of the next tweet to process. This assumes the CSV doesn't change between runs, which is valid since extraction and analysis are separate phases.

The alternative, tracking individual processed tweet IDs would handle dataset changes better but requires state serialization, more memory, and complex resume logic. The simple approach works fine for this use case.

## Fail-Fast Error Handling

Errors immediately stop processing instead of retrying automatically. When a batch fails, the checkpoint doesn't update, so the next cron run retries that batch automatically.

This removes the need for retry policies, backoff algorithms, and partial state tracking. It assumes cron provides sufficient retry semantics, which works for batch jobs where immediate completion isn't critical.
