# Contributing

Thanks for your interest in ai-pr-review-bot. This guide covers local setup,
conventions, and the review process.

## Local setup

```bash
docker compose up -d        # PostgreSQL, Redis, Kafka
cp .env.example .env        # fill in keys
./mvnw spring-boot:run
```

## Branching & commits

- Never commit to `main` directly. Branch per change:
  - `feature/<short-description>` · `fix/<short-description>` · `chore/<...>`
- Phases are developed on `feature/phaseN-<name>` branches and merged via PR.
- Commit messages follow **Conventional Commits**:
  ```
  feat(webhook): validate X-Hub-Signature-256
  fix(cache): fail open when Redis is unreachable
  test(kafka): cover DLT routing after 3 retries
  chore(deps): pin spring-ai to 1.0.0
  ```

## Quality gates (must pass before merge)

```bash
./mvnw verify     # unit + integration tests + JaCoCo 80% line coverage
```

- New code requires tests; the build fails below 80% line coverage.
- Integration tests use Testcontainers (no mocked infrastructure) and are named
  `*IT`. Unit tests are `*Test` and must not require Docker.
- No hardcoded secrets — everything comes from environment variables.
- Public service methods carry a Javadoc comment.

## Code style

- Java 21, 4-space indent, no wildcard imports.
- Constructor injection (Lombok `@RequiredArgsConstructor`), no field injection.
- Validate at boundaries (controllers, Kafka handlers); never trust external data.
- Keep files focused; split by responsibility before they grow large.

## Pull requests

1. Ensure `./mvnw verify` is green and CI passes.
2. Describe what changed and why; link the relevant roadmap phase.
3. Update `CHANGELOG.md` and `docs/ROADMAP.md` when a phase lands.
