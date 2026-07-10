---
paths:
  - "src/**/*.kt"
---

# Kotlin backend conventions

Enforceable checklist for `src/**`. Full context in `CLAUDE.md`. Stack: Kotlin 2.1,
Spring Boot 3.5, JPA/Hibernate, PostgreSQL + Flyway, Testcontainers.

## Architecture (layering)
- Dependencies flow inward, never circular: `api/rest` (inbound) -> `application`
  (domain) <- `infrastructure` (outbound/config).
- Controllers call services, never repositories directly; keep controllers thin
  (map request -> service -> DTO). Services own business logic.
- `infrastructure/` provides beans only - never calls business logic.
- New feature: `application/{feature}` (Entity, Repository, Facade, Service) +
  `api/rest/{feature}` (Controller + `dto/`). Feature deps only point downward.

## Kotlin style
- Prefer expression bodies for single-expression functions.
- No `!!` - model nullability, use `?.` / `?:` / `requireNotNull`.
- Immutable by default: `val` over `var`, read-only collections in signatures.
- `data class` for DTOs/value holders; response DTOs live in `api/rest/{feature}/dto/`.
- Expected/domain failures use domain exceptions in `application/common/` mapped to
  HTTP by controller advice - not ad-hoc `RuntimeException`.
- ktlint is the formatter of record: `ktlintCheck` must pass (`ktlintFormat` to fix).

## Persistence
- Entities implement `Persistable<UUID>` with `@PostPersist`/`@PostLoad` isNew tracking;
  IDs are non-nullable.
- Auditing via `@CreatedDate`/`@LastModifiedDate` against the injected `Clock` bean -
  never `Instant.now()` directly (keeps tests deterministic).
- Schema changes are Flyway migrations - never `ddl-auto` in real profiles.

## API
- No `/api` prefix - endpoints at root. Secure with scope-based `@PreAuthorize`
  (Auth0 OAuth2, toggleable via `jobhunter.auth0.enabled`).
- Every endpoint has typed request/response DTOs; never leak entities over the wire.

## Testing
- JUnit 5 + Testcontainers (Postgres) for integration. Build entities/DTOs via the
  `TestFixtures` factory - do not hand-roll them.
- Test profile `application-test.yml` (Flyway on, auth off). Assert response shape with
  field-scoped `jsonPath`, not full-body equality.
- Add a regression test with every bugfix and a unit test for new mapping logic.
