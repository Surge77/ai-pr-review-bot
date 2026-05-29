# Changelog

All notable changes to this project are documented here. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/); the project uses
[Semantic Versioning](https://semver.org/).

## [Unreleased]

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
