# job-hunter-api

**TL;DR:** Kotlin Spring Boot backend for [Job Hunter](https://github.com/mshykhov/job-hunter). REST API for job vacancy management, AI-powered matching, user preferences.

> **Stack**: Kotlin 2.1, Spring Boot 3.5, PostgreSQL 16, Flyway, Anthropic SDK, Testcontainers

---

## Portfolio Project

**Public repository.** Everything must be clean and professional.

### Standards
- **English only** — README, commits, CLAUDE.md, code, comments
- **Meaningful commits** — conventional commits
- **No junk** — no TODO-hacks, commented-out code in master
- **No AI mentions** in commits

---

## Architecture

### Three-Layer Structure

```
api/rest/            → HTTP layer (inbound)
application/         → Business domain (grouped by feature)
infrastructure/      → Technical concerns (outbound + config)
```

### Dependency Flow

```
api/rest  →  application  ←  infrastructure
(inbound)     (domain)       (outbound + config)
```

**Rules:**
- `api/` depends on `application/` — controllers call services
- `application/` may use beans from `infrastructure/` — services use clients, properties
- `infrastructure/` provides beans only — never calls business logic
- **No circular dependencies** — within `application/`, dependencies flow one-way

### Feature Dependency Graph (application/)

```
matching → job, userjob, preference
criteria → preference
userjob  → user, job
preference → user
job      → (standalone)
user     → (standalone)
```

### Package Structure

```
com.mshykhov.jobhunter/
├── api/rest/                      # HTTP layer
│   ├── {feature}/                 # Controller + dto/ per feature
│   └── exception/                 # GlobalExceptionHandler, ErrorResponse
├── application/                   # Business domain
│   ├── {feature}/                 # Service + Entity + Repository + Facade together
│   └── common/                    # Shared: NotFoundException, ValueMappedEnum, utils
└── infrastructure/                # Technical concerns
    ├── ai/                        # ClaudeClient, AiProperties, AiConfig
    ├── security/                  # SecurityConfig, Auth0Properties
    └── config/                    # OpenApi, Clock, JpaAuditing, Scheduling, Web
```

### Adding a New Feature

1. Create `application/{feature}/` — Entity, Repository, Facade, Service
2. Create `api/rest/{feature}/` — Controller + `dto/` subfolder
3. Ensure dependencies only point downward in the graph (no cycles)

---

## Patterns & Conventions

### Kotlin Style
- **ktlint enforced** — run `./gradlew ktlintFormat` before committing
- **Trailing commas** — always use trailing commas in parameter lists
- **val over var** — mutable only for JPA fields that genuinely change
- **data class** for DTOs — immutable, auto equals/hashCode
- **class** for entities — JPA entities are NOT data classes

### Naming
- Entity: `{Name}Entity` (e.g., `JobEntity`)
- Repository: `{Name}Repository`
- Facade: `{Name}Facade`
- Service: `{Name}Service`
- Controller: `{Name}Controller`
- DTO request: `{Name}Request` (e.g., `JobIngestRequest`)
- DTO response: `{Name}Response` (e.g., `JobResponse`)

### Internal Layering (within each feature)
```
Controller → Service → Facade → Repository
```

- Controller NEVER accesses Repository or Facade directly
- Service NEVER accesses Repository directly (only via Facade)
- Facade is thin: only @Transactional + repository calls

### Database
- **Flyway only** — all schema changes via `V{N}__{description}.sql`
- **Hibernate ddl-auto: validate** — Hibernate validates, never modifies schema
- **Naming**: snake_case for tables/columns, entity fields are camelCase
- **UUID primary keys** — `gen_random_uuid()` in PostgreSQL

### REST API
- No `/api` prefix — endpoints at root path
- Validation via `@Valid` + Bean Validation annotations
- **Auth0 OAuth2** — scope-based `@PreAuthorize`, toggleable via `jobhunter.auth0.enabled`
- **Persistable\<UUID\>** pattern — non-nullable IDs, `@PostPersist`/`@PostLoad` for isNew tracking
- **JPA Auditing** — `@CreatedDate`/`@LastModifiedDate` with custom `Clock` bean

### Testing
- **Unit tests** — MockK for mocking, JUnit 5
- **Integration tests** — Testcontainers with real PostgreSQL
- **Test profile** — `application-test.yml` disables Flyway, uses `ddl-auto: create-drop`

---

## Key Endpoints

| Method | Path | Scope | Description |
|--------|------|-------|-------------|
| `POST` | `/jobs/ingest` | `write:jobs` | Batch ingest jobs from n8n |
| `GET` | `/jobs` | `read:jobs` | User's matched jobs |
| `PATCH` | `/jobs/{id}/status` | `write:jobs` | Update job status (NEW/APPLIED/IRRELEVANT) |
| `GET` | `/criteria?source={SOURCE}` | `read:criteria` | Aggregated search criteria for n8n |
| `GET` | `/preferences` | `read:preferences` | User preferences |
| `PUT` | `/preferences` | `write:preferences` | Save preferences |
| `POST` | `/preferences/normalize` | `write:preferences` | AI-normalize raw text |

---

## Local Development

```bash
cp .env.example .env
docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=local'
```

## Quality Checks

```bash
./gradlew test
./gradlew ktlintCheck
```

## Deployment

- Local: `docker compose up -d` (PostgreSQL only)
- Docker: `docker build -t job-hunter-api .`
- Production: Helm values in `helm/`, deployed via ArgoCD
