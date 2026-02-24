# job-hunter-api

**TL;DR:** Kotlin Spring Boot backend for [Job Hunter](https://github.com/mshykhov/job-hunter). REST API for job vacancy management + Telegram bot notifications.

> **Stack**: Kotlin 2.1, Spring Boot 3.5, PostgreSQL 16, Flyway, Testcontainers

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

### Layer Diagram

```
┌─────────────────────────────────────────────────┐
│ CONTROLLER (REST API)                           │
│ - @RestController                               │
│ - Request validation, DTO mapping               │
│ - Knows only Service layer                      │
└────────────┬────────────────────────────────────┘
             │
┌────────────▼────────────────────────────────────┐
│ SERVICE (Business logic)                        │
│ - @Service                                      │
│ - Orchestrates facades & external integrations  │
│ - Depends on Facade, never on Repository        │
└────────────┬────────────────────────────────────┘
             │
┌────────────▼────────────────────────────────────┐
│ FACADE (Transactional wrapper)                  │
│ - @Component + @Transactional                   │
│ - Thin layer: DB operations with tx boundaries  │
│ - One facade per aggregate                      │
└────────────┬────────────────────────────────────┘
             │
┌────────────▼────────────────────────────────────┐
│ REPOSITORY (Spring Data JPA)                    │
│ - Interface extends JpaRepository               │
│ - Custom @Query when needed                     │
│ - Never called directly from Service            │
└─────────────────────────────────────────────────┘
```

### Package Structure

```
com.mshykhov.jobhunter/
├── config/              # Spring @Configuration beans
├── controller/          # REST controllers
├── service/             # Business logic
├── persistence/
│   ├── model/           # @Entity classes, enums
│   ├── repository/      # Spring Data JPA interfaces
│   └── facade/          # @Transactional facades
├── model/
│   └── dto/             # Request/Response DTOs
└── exception/           # Custom exceptions
```

---

## Patterns & Conventions

### Kotlin Style
- **ktlint enforced** — run `./gradlew ktlintFormat` before committing
- **Trailing commas** — always use trailing commas in parameter lists
- **val over var** — mutable only for JPA fields that genuinely change
- **data class** for DTOs — immutable, auto equals/hashCode
- **class** for entities — JPA entities are NOT data classes
- **Sealed interfaces** for operation results when multiple outcomes exist
- **Extension functions** for mapping (entity ↔ DTO)

### Naming
- Entity: `{Name}Entity` (e.g., `JobEntity`)
- Repository: `{Name}Repository`
- Facade: `{Name}Facade`
- Service: `{Name}Service`
- Controller: `{Name}Controller`
- DTO request: `{Name}Request` (e.g., `JobIngestRequest`)
- DTO response: `{Name}Response` (e.g., `JobResponse`)
- Exception: `{Name}Exception` (e.g., `JobNotFoundException`)

### Database
- **Flyway only** — all schema changes via `V{N}__{description}.sql`
- **Hibernate ddl-auto: validate** — Hibernate validates, never modifies schema
- **Naming**: snake_case for tables/columns, entity fields are camelCase
- **UUID primary keys** — `gen_random_uuid()` in PostgreSQL

### REST API
- Base path: `/api/jobs`
- Ingest endpoint: `POST /api/jobs/ingest` (called by n8n workflows)
- Validation via `@Valid` + Bean Validation annotations
- Error responses: structured JSON with status, message, timestamp

### Testing
- **Unit tests** — MockK for mocking, JUnit 5
- **Integration tests** — Testcontainers with real PostgreSQL
- **Test profile** — `application-test.yml` disables Flyway, uses `ddl-auto: create-drop`

### Dependencies Flow
```
Controller → Service → Facade → Repository
                ↓
          External APIs (Telegram, etc.)
```

**Rules:**
- Controller NEVER accesses Repository or Facade directly
- Service NEVER accesses Repository directly (only via Facade)
- Facade is thin: only @Transactional + repository calls
- No circular dependencies

---

## Key Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/jobs/ingest` | Receive jobs from n8n |
| `GET` | `/api/jobs` | List jobs with filters |
| `PATCH` | `/api/jobs/{id}/status` | Update job status |
| `GET` | `/actuator/health` | Health check |

---

## Local Development

```bash
cp .env.example .env
docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=local'
```

## Testing

```bash
./gradlew test
./gradlew ktlintCheck
```

## Deployment

- Local: `docker compose up -d` (PostgreSQL only)
- Docker: `docker build -t job-hunter-api .`
- Production: Helm values in `helm/`, deployed via ArgoCD
