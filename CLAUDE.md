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
- **Auth0 OAuth2** — scope-based `@PreAuthorize`, toggleable via `jobhunter.auth0.enabled`
- **BYOK AI** — each user stores an encrypted OpenAI-compatible API key + model id; per-user clients via `ChatClientFactory`
- **Persistable\<UUID\>** — non-nullable IDs, `@PostPersist`/`@PostLoad` for isNew tracking
- **JPA Auditing** — `@CreatedDate`/`@LastModifiedDate` with custom `Clock` bean
- **Test fixtures** — `TestFixtures` object with factory methods for entities and DTOs
- **Test profile** — `src/test/resources/application-test.yml` (Flyway on, auth off)
- **AI DTOs** in `application/ai/dto/`, domain exceptions in `application/common/`

## Docs

- `docs/README.md` — doc index
- `docs/job-matching-architecture.md` — matching pipeline (cold filter → AI scoring)
