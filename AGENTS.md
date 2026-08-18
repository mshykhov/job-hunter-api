# Job Hunter API

Kotlin Spring Boot backend for Job Hunter. It owns vacancy ingestion, matching,
user preferences, outreach, persistence, and user-facing REST endpoints.

Stack: Kotlin 2.1, Spring Boot 3.5, Spring AI 1.1, PostgreSQL 16, Flyway, and
Testcontainers.

## Commands

```sh
./gradlew build
./gradlew test
./gradlew ktlintFormat
./gradlew bootRun
npm run rulesync:verify
```

`build` compiles, runs ktlint checks, and executes tests. Integration tests require
Docker for Testcontainers. Local boot requires PostgreSQL and the documented local
profile configuration.

## Architecture

Dependencies flow inward:

```text
api/rest -> application <- infrastructure
```

- `api/rest/` is the inbound HTTP boundary. Controllers map requests, call services,
  and return typed DTOs.
- `application/` owns business behavior grouped by feature.
- `infrastructure/` owns adapters, persistence configuration, security, providers,
  observability, and other technical concerns.
- Controllers never call repositories directly.
- Preserve the current use of injected infrastructure beans and API DTOs from
  application services unless a task explicitly changes those boundaries.
- Avoid new dependency cycles. The existing AI/outreach configuration cycle is known
  debt and must not be deepened.

## Project contracts

- Endpoints have no `/api` prefix.
- Authorization uses multi-issuer OIDC and scope-based method security.
- User AI providers are ordered and fall through on failure; provider keys are
  encrypted, while `CODEX` requires no key.
- JPA entities use non-null UUIDs and `Persistable<UUID>` new-state tracking.
- Auditing uses the injected `Clock` so tests remain deterministic.
- Test fixtures come from the shared `TestFixtures` factory.
- The test profile keeps Flyway enabled and authentication disabled.
- AI DTOs live under `application/ai/dto/`; domain exceptions live under
  `application/common/`.

## Documentation and configuration

Read `docs/README.md` and the relevant architecture or runbook document before
changing matching, providers, retention, or operational behavior. Keep living docs
aligned with their code.

`.rulesync/` is canonical. Generated instruction and scoped-rule files are derived
outputs and must not be edited directly.

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
