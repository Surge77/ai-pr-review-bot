# Changelog

All notable changes to this project are documented here. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/); the project uses
[Semantic Versioning](https://semver.org/).

## [Unreleased]

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
