---
root: false
globs:
  - 'src/**/*.kt'
---
# Kotlin backend conventions

Apply this checklist to Kotlin source. The repository root instructions provide the
architecture context.

## Architecture

- Dependencies flow inward: `api/rest` -> `application` <- `infrastructure`.
- Keep controllers thin: map request -> service -> DTO. Services own business logic.
- Infrastructure provides adapters and beans; it does not invoke business flows.
- A new feature normally uses `application/{feature}` plus
  `api/rest/{feature}` with a `dto/` subdirectory.
- Preserve the established feature dependencies:
  - `matching` -> ai, job, preference, userjob
  - `outreach` -> ai, job, preference, user, userjob
  - `ai` -> job, preference, user, settings, and the known outreach configuration cycle
  - `preference` -> ai, job, user
  - `criteria` -> job, preference
  - `userjob` -> job, user
  - `proxy` -> job
  - `settings` and `user` remain standalone

## Kotlin

- Prefer expression bodies for single-expression functions.
- Model nullability instead of using `!!`.
- Prefer `val` and read-only collections.
- Use data classes for DTOs and value holders.
- Put expected domain failures in `application/common/` and map them through
  controller advice rather than throwing ad hoc runtime exceptions.
- Run `ktlintCheck`; use `ktlintFormat` to fix formatting.

## Persistence and API

- Entities implement `Persistable<UUID>` with `@PostPersist` and `@PostLoad`
  new-state tracking. IDs are non-null.
- Use `@CreatedDate` and `@LastModifiedDate` with the injected `Clock`; do not call
  `Instant.now()` directly.
- Make schema changes through Flyway migrations. Do not rely on `ddl-auto` outside
  disposable development environments.
- Use typed request and response DTOs. Never expose persistence entities directly.
- Protect endpoints with the established scope-based authorization pattern.

## Tests

- Use JUnit 5 and Testcontainers for integration coverage.
- Build entities and DTOs through `TestFixtures`.
- Use `application-test.yml` with Flyway enabled and authentication disabled.
- Assert response shapes with field-scoped `jsonPath` checks.
- Add a regression test for every bug fix and unit coverage for new mapping logic.
