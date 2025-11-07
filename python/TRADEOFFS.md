# Technical Tradeoffs & Design Decisions

## Language Choice: Python

Python makes prototyping fast and has first-class support for AI integrations. Google's `generativeai` SDK is Python-native, and the language's dynamic typing makes it easy to iterate on prompt engineering and response parsing.

The downside is runtime performance and deployment complexity. Dependencies must be managed via Poetry instead of shipping a single binary, and the interpreter overhead means slower execution and higher memory usage.

## Architecture: Long-Running Process

The tool runs as a single long-running process that completes all batches in one execution. It saves checkpoints after each batch internally, so if interrupted (Ctrl+C, crash, etc.), rerunning picks up where it left off.

This is simpler for users: no cron jobs or manual re-runs needed, but it creates longer-running processes that are harder to monitor and can't be easily scheduled with different resource constraints per batch.

Sequential processing trades speed for simplicity. It naturally rate-limits API calls (configurable delay between requests), makes debugging straightforward (linear execution), and checkpoint logic is simple. Concurrent processing would be 5-10x faster but adds retry coordination, partial failure handling, and complex state management.

## Output Format: Deletion Candidates Only

The results CSV only contains tweets marked for deletion, not all analyzed tweets. Each row includes the tweet URL and a `deleted` column initialized to `false`, allowing users to manually track deletion progress.

This minimizes output file size (only action items, not the full corpus) and gives users a focused checklist. The tradeoff is losing a complete audit trail of all decisions. If the analysis needs to be reviewed later, the tool must be re-run.

## Retry Logic at Service Boundary

The Gemini analyzer includes exponential backoff retry for transient API errors (429, 503, timeouts), but the application layer doesn't retry on failures. When a batch fails, the checkpoint doesn't update, so rerunning resumes from that batch.

This handles API flakiness automatically while keeping application logic simple. Persistent failures or logic errors stop processing immediately, and the checkpoint mechanism provides retry semantics without complex application-level policies.
