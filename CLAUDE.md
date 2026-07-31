# job-hunter-api

Kotlin Spring Boot backend for [Job Hunter](https://github.com/mshykhov/job-hunter). REST API for job vacancy management, AI-powered matching, user preferences, outreach generation.

> **Stack**: Kotlin 2.1, Spring Boot 3.5, Spring AI 1.0 (OpenAI-compatible), PostgreSQL 16, Flyway, Testcontainers

## Commands

```bash
./gradlew build          # compile + ktlintCheck + tests (Testcontainers needs Docker)
./gradlew test           # tests only
./gradlew ktlintFormat   # fix formatting before commit
./gradlew bootRun        # run locally (needs Postgres, see application.yml)
```

## Architecture

Three-layer structure — dependencies flow inward, never circular:

```
api/rest  →  application  ←  infrastructure
(inbound)     (domain)       (outbound + config)
```

- `api/rest/` — controllers call services, DTOs in `dto/` subfolder
- `application/` — business domain grouped by feature (Entity + Repository + Facade + Service together)
- `infrastructure/` — provides beans only, never calls business logic
- `application/` may use beans from `infrastructure/` and return DTOs from `api/rest/`

### Feature Dependencies (application/)

`common` is shared utils, omitted from the graph:

```
matching   → ai, job, preference, userjob
outreach   → ai, job, preference, user, userjob
ai         → job, preference, user (+ outreach config — known cycle, do not deepen it)
preference → ai, job, user
ai         → settings (provider/model catalogue)
criteria   → job, preference
userjob    → job, user
proxy      → job
settings, user → (standalone)
```

### Adding a New Feature

1. Create `application/{feature}/` — Entity, Repository, Facade, Service
2. Create `api/rest/{feature}/` — Controller + `dto/` subfolder
3. Ensure dependencies only point downward (no cycles)

## Project-Specific Patterns

- No `/api` prefix — endpoints at root path
- **Authentik OIDC** — multi-issuer JWT (`jobhunter.oidc.issuers`), scope-based `@PreAuthorize`, toggleable via `jobhunter.oidc.enabled`
- **AI provider chain** - ordered per-user providers (`user_ai_providers`), tried in priority order and falling through on failure; keys encrypted, `CODEX` needs none
- **Persistable\<UUID\>** — non-nullable IDs, `@PostPersist`/`@PostLoad` for isNew tracking
- **JPA Auditing** — `@CreatedDate`/`@LastModifiedDate` with custom `Clock` bean
- **Test fixtures** — `TestFixtures` object with factory methods for entities and DTOs
- **Test profile** — `src/test/resources/application-test.yml` (Flyway on, auth off)
- **AI DTOs** in `application/ai/dto/`, domain exceptions in `application/common/`

## Docs

- `docs/README.md` — doc index
- `docs/runbook.md` - diagnosing matching, alerts, re-matching a period
- `docs/job-matching-architecture.md` — matching pipeline (cold filter → AI scoring)
