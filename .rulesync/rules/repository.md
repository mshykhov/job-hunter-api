---
root: true
---
# Job Hunter API

Kotlin Spring Boot backend for Job Hunter. It owns vacancy ingestion, matching,
user preferences, outreach, persistence, and user-facing REST endpoints.

Stack: Kotlin 2.1, Spring Boot 3.5, Spring AI 1.0, PostgreSQL 16, Flyway, and
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
