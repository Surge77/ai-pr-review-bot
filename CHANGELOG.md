# Changelog

All notable changes to this project are documented here. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/); the project uses
[Semantic Versioning](https://semver.org/).

## [Unreleased]

## [0.6.1] - 2026-05-30
### Added — Phase 6.5: review orchestrator
- `ReviewOrchestrator` (`@Primary` `ReviewProcessor`): wires the per-file pipeline —
  fetch changed files → filter binaries → cache-check (skip) → LLM review → audit,
  then cache the result. Per-file failures are isolated and audited FAILED.
- `AuditLogService.recordReviewed` / `recordSkipped` / `recordFileFailure` persist
  per-file outcomes (status, feedback JSON, issue count, critical flag).
- Tests: orchestrator paths (review/skip/fail/non-reviewable) and the new audit methods.

## [0.6.0] - 2026-05-30
### Added — Phase 6: LLM review via Spring AI
- `LLMReviewService`: prompts the chat model per file, timeout-bounded (30s, configurable)
  on a virtual thread; failures return empty so each file can be marked FAILED in isolation.
- `PromptTemplateLoader`: loads `prompts/code-review.st`, literal `{filename}`/`{patch}`
  substitution (preserves JSON braces).
- `ReviewFeedbackParser`: extracts the JSON object (tolerant of markdown fences/prose),
  graceful `ReviewFeedback.fallback()` on malformed output — never throws.
- Models: `ReviewFeedback`, `ReviewIssue`, `Severity` (lenient: unknown → SUGGESTION).
- `LlmConfig` (ChatClient bean), `LlmProperties` (`app.llm.timeout-seconds`).
- Tests: parser (valid/fenced/unknown-severity/malformed), prompt loader, service
  (mocked ChatClient: success + failure isolation).
### Changed
- Default Groq model `qwen-2.5-coder-32b` → `llama-3.3-70b-versatile` (former not
  available on Groq; latter verified live).

## [0.5.0] - 2026-05-29
### Added — Phase 5: Redis cache (skip unchanged files)
- `CacheKeyStrategy`: `review:{repoFullName}:{filename}:{sha256(patch)}` keys.
- `CacheCheckService`: cache hit → skip LLM; `markReviewed` stores keys with a 7-day TTL.
- Fail-open: Redis unavailable → review anyway (warn); `added` files always bypass.
- Tests: key generation, hit/miss, fail-open, and TTL (unit); Testcontainers Redis
  hit-after-mark and added-bypass (CI).

## [0.4.0] - 2026-05-29
### Added — Phase 4: GitHub diff fetching & parsing
- `GitHubApiClient` (Spring `RestClient`, PAT bearer auth): fetches PR changed files
  from `/repos/{owner}/{repo}/pulls/{n}/files`; rate-limit aware — backs off until
  `X-RateLimit-Reset` when `X-RateLimit-Remaining < 10`.
- `Sleeper` abstraction (+ `ThreadSleeper`) so backoff is unit-testable without delay.
- `DiffParserService`: filters binary/empty patches; splits patches > 3000 chars into
  overlapping 2500-char chunks (200 overlap).
- `FileDiff` and `DiffChunk` models; `GitHubProperties` typed config; `githubRestClient` bean.
- Tests: chunking + filtering (unit); WireMock-stubbed fetch, field mapping, and
  rate-limit backoff behavior.

## [0.3.0] - 2026-05-29
### Added — Phase 3: Kafka pipeline
- Topic declarations: `pr.review.requested` and `pr.review.failed` (3 partitions, RF 1).
- `ReviewRequestConsumer` (group `pr-review-group`, manual `MANUAL_IMMEDIATE` ack):
  hands events to a `ReviewProcessor` seam and commits only on success.
- Retry + dead-letter: `DefaultErrorHandler` with `ExponentialBackOffWithMaxRetries(3)`
  and a `DeadLetterPublishingRecoverer` routing exhausted records to `pr.review.failed`.
- `ReviewFailedConsumer` (group `pr-review-dlt-group`): logs and records a `FAILED`
  audit entry via `AuditLogService`.
- `ReviewAuditLog` JPA entity (+ `ReviewStatus` enum), `ReviewAuditLogRepository`,
  `AuditLogService.recordFailure`.
- Producer now routes send failures to the dead-letter topic.
- Tests: consumer handlers, DLT consumer, audit service, event serialization; plus a
  Testcontainers `KafkaPipelineIT` (consume→process and failure→DLT→audit, CI-only).

## [0.2.0] - 2026-05-29
### Added — Phase 2: GitHub webhook receiver
- `POST /api/webhooks/github`: reads the raw body before deserialization,
  validates `X-Hub-Signature-256` (HMAC-SHA256, constant-time compare), filters to
  accepted PR actions, and publishes valid events to Kafka — always answering fast.
- `GitHubSignatureValidator` (standalone, testable) and `WebhookPayloadParser`
  (lenient GitHub-JSON → `PullRequestEvent` mapping).
- `PullRequestEvent` model with `partitionKey()` and `X-GitHub-Delivery` idempotency id.
- `PullRequestEventPublisher` abstraction + Kafka implementation (keyed JSON to
  `pr.review.requested`); `KafkaTopics` registry.
- `WebhookProperties` typed config (`app.webhook.secret`, `accepted-actions`);
  Kafka producer JSON serializers.
- Tests: signature validator, payload parser, controller (all branches), and a
  publisher test producing to an in-JVM Kafka broker (no Docker).

## [0.1.0] - 2026-05-29
### Added — Phase 1: Project skeleton & infrastructure
- Spring Boot 3.4 / Java 21 Maven project with the full dependency set.
- Package structure under `com.aireviewer` (webhook, kafka, diff, cache, llm,
  github, audit, websocket, config, model, exception, health).
- `docker-compose.yml` for PostgreSQL 15, Redis 7, and Kafka 3.8 (KRaft mode).
- Flyway migration `V1__init.sql` creating the unified `review_audit_log` table.
- `application.yml` with `local` (Groq) and `prod` (Gemini) profiles; LLM provider
  swap is config-only via the OpenAI-compatible API.
- Aggregate `GET /health` endpoint probing PostgreSQL, Redis, and Kafka.
- OpenAPI/Swagger UI, Spring Boot Actuator, and a JaCoCo 80% line-coverage gate.
- Unit tests for the health controller and probe service; `ApplicationContextIT`
  integration test (Testcontainers: PostgreSQL, Kafka, Redis).
