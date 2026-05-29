# Roadmap

The bot is built in phases. Each phase ships with tests and is merged via its own
branch/PR. Status is tracked here and in [CHANGELOG.md](../CHANGELOG.md).

| Phase | Title                              | Status |
|-------|------------------------------------|--------|
| 1     | Project skeleton & infrastructure  | ✅ Done |
| 2     | GitHub webhook receiver (HMAC)     | ✅ Done |
| 3     | Kafka pipeline (producer/consumer/DLT) | ✅ Done |
| 4     | GitHub diff fetching & parsing     | ✅ Done |
| 5     | Redis cache (skip unchanged files) | ✅ Done |
| 6     | LLM review via Spring AI           | ✅ Done |
| 6.5   | Consumer orchestrator (wires 4→9)  | ✅ Done |
| 7     | Post inline comments to PR         | ⬜ Planned |
| 8     | WebSocket live progress dashboard  | ⬜ Planned |
| 9     | Audit log & reporting API          | ⬜ Planned |
| 10    | Production hardening               | ⬜ Planned |

## Phase 1 — delivered

- Maven project, Spring Boot 3.4, Java 21, full dependency set.
- Package structure under `com.aireviewer`.
- `docker-compose.yml`: PostgreSQL 15, Redis 7, Kafka 3.8 (KRaft).
- Flyway `V1__init.sql`: unified `review_audit_log` schema.
- `application.yml` with `local` (Groq) and `prod` (Gemini) profiles.
- `GET /health` aggregate endpoint (DB + Redis + Kafka).
- OpenAPI/Swagger UI, Actuator, JaCoCo 80% coverage gate.
- Tests: health controller + probe unit tests; `ApplicationContextIT` (Testcontainers).

## Design notes that shaped the plan

- **Ollama dropped** in favor of hosted free LLM APIs (Groq/Gemini) so the project
  runs on any laptop without a GPU and `docker compose up` stays light.
- **Unified schema in V1** (no throwaway migrations): the original `fraud_score`
  column was dropped in favor of `issues_found` / `has_critical` / `delivery_id`.
- **`*IT` vs `*Test`** split so unit tests run without Docker and integration tests
  (Testcontainers) run under Failsafe in `mvn verify` / CI.
