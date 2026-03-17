# job-hunter-api

Kotlin Spring Boot backend for [Job Hunter](https://github.com/mshykhov/job-hunter). REST API for job vacancy management, AI-powered matching, user preferences.

> **Stack**: Kotlin 2.1, Spring Boot 3.5, PostgreSQL 16, Flyway, Anthropic SDK, Testcontainers

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

```
matching → job, userjob, preference
criteria → preference
userjob  → user, job
preference → user
job, user  → (standalone)
```

### Adding a New Feature

1. Create `application/{feature}/` — Entity, Repository, Facade, Service
2. Create `api/rest/{feature}/` — Controller + `dto/` subfolder
3. Ensure dependencies only point downward (no cycles)

## Project-Specific Patterns

- No `/api` prefix — endpoints at root path
- **Auth0 OAuth2** — scope-based `@PreAuthorize`, toggleable via `jobhunter.auth0.enabled`
- **Persistable\<UUID\>** — non-nullable IDs, `@PostPersist`/`@PostLoad` for isNew tracking
- **JPA Auditing** — `@CreatedDate`/`@LastModifiedDate` with custom `Clock` bean
- **Test fixtures** — `TestFixtures` object with factory methods for entities and DTOs
- **Test profile** — `src/test/resources/application-test.yml` (Flyway on, auth off)
- **AI DTOs** in `application/ai/dto/`, domain exceptions in `application/common/`
