# Architecture

This document explains how the pipeline fits together and the reasoning behind
the key design decisions. For the build order see [ROADMAP.md](ROADMAP.md).

## Why event-driven?

GitHub webhooks must be answered fast (GitHub times out and retries slow
deliveries). The controller does only cheap work — verify the HMAC signature and
publish an event — then returns `200` immediately. All expensive work (network
calls to GitHub, LLM inference, DB writes) happens asynchronously off a Kafka
consumer. This decouples ingestion from processing and lets us retry, scale
consumers, and isolate poison messages in a dead-letter topic.

## Components

| Package      | Responsibility                                                                 |
|--------------|--------------------------------------------------------------------------------|
| `webhook`    | Raw-body capture, HMAC-SHA256 validation, event filtering, Kafka handoff.      |
| `kafka`      | Producer (keyed JSON), consumer (manual commit, retry+backoff), dead-letter.   |
| `github`     | `RestClient` to GitHub: fetch PR files, post comments, rate-limit backoff.     |
| `diff`       | Parse PR files into per-file chunks; filter binaries; chunk large patches.     |
| `cache`      | SHA-256 keyed Redis lookups to skip unchanged files; fail-open on Redis down.  |
| `llm`        | Spring AI `ChatClient` + structured prompt; parse to `ReviewFeedback`.         |
| `audit`      | Persist one row per file to PostgreSQL; paginated reporting + stats endpoints.  |
| `websocket`  | STOMP config + publisher streaming per-stage progress to the dashboard.        |
| `health`     | Aggregate `/health` endpoint probing DB, Redis, Kafka.                          |
| `config`     | Typed config properties, OpenAPI metadata, shared beans.                        |
| `model`      | JPA entities, Kafka event payloads, DTOs, `ApiResponse<T>` envelope.            |
| `exception`  | Exception types + global `@ControllerAdvice`.                                   |

## Key decisions

### LLM provider abstraction
The OpenAI, Groq, and Gemini APIs are all OpenAI-compatible. By using Spring AI's
`spring-ai-starter-model-openai` and pointing `base-url` at the chosen provider,
swapping providers is a profile change — no code or dependency change. `local`
targets Groq (free, fast); `prod` targets Gemini (free tier).

### Cache: fail-open, not fail-closed
The cache is an optimization, not a source of truth. If Redis is unavailable we
review the file anyway and log a warning. Reviewing twice is cheap; *not*
reviewing because of a cache hiccup is a real failure. New files (`status=added`)
always bypass the cache.

### At-least-once with a dead-letter topic
The consumer commits offsets manually after successful processing and retries
with exponential backoff. Messages that still fail land on `pr.review.failed`,
where they are logged and the corresponding audit record is marked `FAILED`.

### Idempotency
GitHub redelivers webhooks. Each delivery carries an `X-GitHub-Delivery` id which
is persisted (`review_audit_log.delivery_id`) and used to deduplicate so a
redelivery does not double-post comments.

### Schema ownership
Flyway owns the schema (`spring.jpa.hibernate.ddl-auto=validate`); Hibernate never
mutates it. Migrations live in `src/main/resources/db/migration`.

## Data model

`review_audit_log` — one row per file evaluated during a review:

| Column           | Notes                                            |
|------------------|--------------------------------------------------|
| `id`             | surrogate PK                                     |
| `delivery_id`    | GitHub delivery id (idempotency)                 |
| `pr_number`      | PR number                                        |
| `repo_full_name` | `owner/repo`                                     |
| `file_path`      | reviewed file                                    |
| `commit_sha`     | head SHA the review ran against                  |
| `status`         | `REVIEWED` / `SKIPPED` / `FAILED`                |
| `llm_feedback`   | full `ReviewFeedback` JSON (`jsonb`)             |
| `issues_found`   | count of issues from the LLM                     |
| `has_critical`   | true if any `CRITICAL` issue                     |
| `created_at`     | timestamp                                        |

Indexed for the three reporting queries: list-by-repo, files-by-PR, status-stats.
