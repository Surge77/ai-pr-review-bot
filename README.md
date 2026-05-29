# ai-pr-review-bot

> AI-powered GitHub Pull Request code review system — an event-driven Spring Boot
> pipeline that reviews diffs with an LLM and posts inline comments back to the PR.
> Inspired by the core pipeline of tools like CodeRabbit.

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

A GitHub PR is opened → the bot validates the webhook, fetches the diff, skips
files it has already reviewed (Redis cache), asks an LLM to review each changed
file, posts inline comments back to the PR, records an audit trail, and streams
live progress to a dashboard over WebSocket.

---

## Architecture

```
GitHub PR opened / synchronized
        │  POST /api/webhooks/github  (X-Hub-Signature-256)
        ▼
┌──────────────────────┐   HMAC-SHA256 verify, 200 immediately
│  WebhookController    │───────────────────────────────────────┐
└──────────┬───────────┘                                         │ invalid → 401
           │ publish PullRequestEvent                            │
           ▼                                                     │
┌──────────────────────┐   topic: pr.review.requested           │
│   KafkaProducer       │   key = repoFullName + prNumber        │
└──────────┬───────────┘                                         │
           ▼                                                     │
┌──────────────────────┐   group: pr-review-group, manual commit│
│   KafkaConsumer       │   retry x3 + backoff → DLT on failure  │
└──────────┬───────────┘                                         │
           ▼                                                     │
┌──────────────────────┐   GET /repos/.../pulls/{n}/files        │
│  GitHubApiClient +    │   rate-limit aware (RestClient)         │
│  DiffParserService    │   chunk large patches                  │
└──────────┬───────────┘                                         │
           ▼                                                     │
┌──────────────────────┐   SHA-256(patch) key, TTL 7d            │
│  CacheCheckService    │   hit → SKIP (no LLM)  ·  fail-open     │
└──────────┬───────────┘                                         │
           ▼                                                     │
┌──────────────────────┐   Spring AI ChatClient                  │
│  LLMReviewService     │   structured ReviewFeedback (JSON)      │
└──────────┬───────────┘                                         │
           ▼                                                     │
┌──────────────────────┐   inline + summary comments             │
│  GitHubCommentService │   422 → general · 403 → backoff         │
└──────────┬───────────┘                                         │
           ▼                                                     ▼
┌──────────────────────┐                          ┌──────────────────────┐
│   AuditLogService     │  PostgreSQL (Flyway)     │  WebSocketPublisher   │
│   review_audit_log    │                          │  /topic/review-progress│
└──────────────────────┘                          └──────────────────────┘
```

**Tech stack:** Java 21 (virtual threads) · Spring Boot 3.4 · Spring AI ·
Apache Kafka (KRaft) · Redis · PostgreSQL + Flyway · Spring WebSocket/STOMP ·
Testcontainers · WireMock · JUnit 5 + Mockito · Lombok · MapStruct · Docker Compose.

**LLM provider is swappable via config only** — `local` profile uses
[Groq](https://console.groq.com) (free), `prod` uses
[Gemini](https://aistudio.google.com) (free tier). Both speak the OpenAI-compatible
API, so switching providers is just `base-url` + key + model in `application.yml`.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for component detail and
[docs/ROADMAP.md](docs/ROADMAP.md) for the phased build plan.

---

## Run locally

**Prerequisites:** JDK 21+, Maven 3.9+, Docker Desktop.

```bash
# 1. Start infrastructure (PostgreSQL, Redis, Kafka)
docker compose up -d

# 2. Configure secrets
cp .env.example .env        # then fill in GROQ_API_KEY + GITHUB_* values

# 3. Run the app (loads the `local` profile by default)
./mvnw spring-boot:run      # Windows: mvnw.cmd spring-boot:run
```

Then open:
- Swagger UI → http://localhost:8080/swagger-ui.html
- Health → http://localhost:8080/health
- Live dashboard → http://localhost:8080/dashboard.html *(added in Phase 8)*

### Tests

```bash
./mvnw test       # unit tests only (no Docker required)
./mvnw verify     # unit + Testcontainers integration tests + 80% coverage gate (needs Docker)
```

Integration tests are named `*IT` and run under Failsafe; `mvn test` skips them.

---

## Test against a real GitHub repo (ngrok)

GitHub must reach your machine, so expose port 8080 with a tunnel:

```bash
# 1. Run the app locally (see above), then:
ngrok http 8080
# ngrok prints a public URL, e.g. https://abcd-1234.ngrok-free.app
```

Configure the webhook on your repo (**Settings → Webhooks → Add webhook**):

| Field        | Value                                                         |
|--------------|---------------------------------------------------------------|
| Payload URL  | `https://<your-ngrok>.ngrok-free.app/api/webhooks/github`     |
| Content type | `application/json`                                            |
| Secret       | the same value as `GITHUB_WEBHOOK_SECRET` in your `.env`       |
| Events       | "Let me select individual events" → **Pull requests**         |

Open or push to a PR in that repo → the bot reviews it and comments back.
Watch progress live at `/dashboard.html` and the audit trail at `/api/audit/...`.

---

## Environment variables

| Variable                 | Required        | Default                                   | Description                                        |
|--------------------------|-----------------|-------------------------------------------|----------------------------------------------------|
| `SPRING_PROFILES_ACTIVE` | no              | `local`                                   | `local` (Groq) or `prod` (Gemini)                  |
| `GROQ_API_KEY`           | local profile   | —                                         | Groq API key (free tier)                           |
| `GEMINI_API_KEY`         | prod profile    | —                                         | Google Gemini API key (free tier)                  |
| `LLM_MODEL`              | no              | `qwen-2.5-coder-32b` / `gemini-2.0-flash` | Chat model id for the active provider              |
| `GITHUB_TOKEN`           | yes (runtime)   | —                                         | PAT with `pull_requests` read+write                |
| `GITHUB_API_BASE_URL`    | no              | `https://api.github.com`                  | Override for GitHub Enterprise                     |
| `GITHUB_WEBHOOK_SECRET`  | yes (runtime)   | —                                         | HMAC-SHA256 shared secret for webhook validation   |
| `AUDIT_API_KEY`          | no              | `local-dev-key`                           | `X-API-Key` required by `/api/audit/**`            |
| `DB_URL`                 | no              | `jdbc:postgresql://localhost:5432/aireviewer` | JDBC URL                                      |
| `DB_USERNAME` / `DB_PASSWORD` | no         | `aireviewer` / `aireviewer`               | PostgreSQL credentials                             |
| `REDIS_HOST` / `REDIS_PORT`   | no         | `localhost` / `6379`                      | Redis connection                                   |
| `KAFKA_BOOTSTRAP_SERVERS`| no              | `localhost:9092`                          | Kafka bootstrap servers                            |

No secret is ever hardcoded; everything is read from the environment (12-factor).

---

## Sample webhook payload (manual testing)

Minimal `pull_request` event the bot acts on (`opened` / `synchronize`):

```json
{
  "action": "opened",
  "number": 42,
  "pull_request": {
    "number": 42,
    "head": { "sha": "f00ba7c0ffee1234567890abcdef1234567890ab" },
    "base": { "sha": "deadbeefcafe1234567890abcdef1234567890ab" },
    "diff_url": "https://github.com/octocat/hello-world/pull/42.diff"
  },
  "repository": { "full_name": "octocat/hello-world", "owner": { "login": "octocat" } },
  "sender": { "login": "octocat" }
}
```

Send it locally with a valid signature (Phase 2 onward). Example using `openssl`:

```bash
BODY=$(cat sample-pr.json)
SIG="sha256=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac "$GITHUB_WEBHOOK_SECRET" | awk '{print $2}')"
curl -X POST http://localhost:8080/api/webhooks/github \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: pull_request" \
  -H "X-Hub-Signature-256: $SIG" \
  --data "$BODY"
```

---

## Project status

Built in phases — see [docs/ROADMAP.md](docs/ROADMAP.md) and [CHANGELOG.md](CHANGELOG.md).
Current: **Phase 1 — skeleton & infrastructure** ✅

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Licensed under [MIT](LICENSE).
