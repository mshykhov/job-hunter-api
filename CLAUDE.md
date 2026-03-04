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
- `application/` may use beans and types from `infrastructure/` — services use clients, properties
- `application/` services may accept and return REST DTOs from `api/rest/` — keeping the API layer thin
- `infrastructure/` provides beans only — never calls business logic
- **No circular dependencies** — within `application/`, dependencies flow one-way

### DTO Placement
- **REST DTOs** live in `api/rest/{feature}/dto/` — request/response classes for HTTP layer
- **AI DTOs** live in `application/ai/dto/` — internal AI service models
- **Domain exceptions** live in `application/common/` — shared across all features
- **Error response DTOs** live in `api/rest/exception/` — HTTP error formatting

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
│   ├── ai/                        # AI evaluators, UserAiSettings (Entity/Facade/Service), ChatClientFactory + dto/
│   ├── profile/                   # ProfileService (read-only aggregation)
│   └── common/                    # Shared: NotFoundException, ServiceUnavailableException, AiNotConfiguredException, ValueMappedEnum, utils
└── infrastructure/                # Technical concerns
    ├── ai/                        # AiConfig, AiProperties, AiEncryptionConverter
    ├── fingerprint/               # BrowserFingerprint, FingerprintProvider, ScrapeOpsProperties
    ├── proxy/                     # WebshareClient, WebshareConfig + model/ for DTOs
    ├── ratelimit/                 # RateLimitFilter
    ├── security/                  # SecurityConfig, AudienceValidator, Auth0Properties, DevAuthenticationFilter
    └── config/                    # OpenApi, Clock, JpaAuditing, Scheduling, Web, Cache
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

### File Organization
- **One class per file** — no multi-class files (except small related sealed interfaces)
- **Infrastructure models** in `model/` subfolder when a package has external API DTOs
- **Split folders** with 10+ files into logical subfolders

### Database
- **Flyway only** — all schema changes via `V{N}__{description}.sql`
- **Hibernate ddl-auto: validate** — Hibernate validates, never modifies schema
- **Naming**: snake_case for tables/columns, entity fields are camelCase
- **UUID primary keys** — `gen_random_uuid()` in PostgreSQL
- **`@Embedded`** for value objects (e.g., `Telegram` in `UserEntity`)

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
| `POST` | `/jobs/check` | `write:jobs` | Smart job change detection |
| `POST` | `/jobs/search` | `read:jobs` | User's matched jobs (cursor-based) |
| `GET` | `/jobs/{id}` | `read:jobs` | Job detail with AI reasoning |
| `PATCH` | `/jobs/{id}/status` | `write:jobs` | Update job status (NEW/APPLIED/IRRELEVANT) |
| `POST` | `/jobs/rematch` | `write:jobs` | Re-trigger AI matching |
| `GET` | `/public/jobs` | — | Public job listing (cached, rate-limited) |
| `GET` | `/criteria?source={SOURCE}` | `read:criteria` | Aggregated search criteria for n8n |
| `GET` | `/preferences` | `read:preferences` | User preferences |
| `PUT` | `/preferences` | `write:preferences` | Save preferences |
| `POST` | `/preferences/normalize` | `write:preferences` | AI-normalize raw text (uses user's AI) |
| `POST` | `/preferences/normalize/file` | `write:preferences` | AI-normalize from PDF/DOCX (uses user's AI) |
| `GET` | `/settings/ai-providers` | `read:settings` | Static AI model catalog |
| `GET` | `/settings/ai` | `read:settings` | User's AI settings (masked key) |
| `PUT` | `/settings/ai` | `write:settings` | Save user's AI settings (API key + model) |
| `GET` | `/profile` | `read:profile` | User profile with warnings |

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
