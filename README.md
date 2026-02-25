# Job Hunter API

Kotlin Spring Boot backend for job vacancy monitoring and tracking. Part of the [Job Hunter](https://github.com/mshykhov/job-hunter) system.

## Overview

REST API that receives scraped job listings from n8n workflows, matches them to users via cold filter + AI (Claude Haiku), and provides user-facing endpoints for job tracking and preference management.

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Kotlin | 2.1 | Language |
| Spring Boot | 3.5 | Framework |
| Spring Data JPA | - | Database access |
| PostgreSQL | 16 | Database |
| Flyway | - | Schema migrations |
| Anthropic SDK | 2.0 | AI filtering (Claude Haiku) |
| Auth0 / OAuth2 | - | Authorization |
| SpringDoc OpenAPI | 2.8 | API documentation |
| Testcontainers | - | Integration testing |

## Quick Start

```bash
# Start PostgreSQL
cp .env.example .env
docker compose up -d

# Run the application
./gradlew bootRun --args='--spring.profiles.active=local'
```

## API Documentation

Swagger UI: [http://localhost:8095/swagger-ui](http://localhost:8095/swagger-ui)

OpenAPI spec: [http://localhost:8095/api-docs](http://localhost:8095/api-docs)

## Project Structure

```
src/main/kotlin/com/mshykhov/jobhunter/
├── api/rest/                  # HTTP layer (controllers + DTOs)
│   ├── job/                   # Job ingest + user job endpoints
│   ├── criteria/              # Search criteria for n8n
│   ├── preference/            # User preference endpoints
│   └── exception/             # Global error handling
├── application/               # Business domain (grouped by feature)
│   ├── job/                   # JobService, JobEntity, JobRepository, JobFacade
│   ├── user/                  # UserEntity, UserRepository, UserFacade
│   ├── userjob/               # UserJobService, UserJobEntity, status tracking
│   ├── preference/            # PreferenceService, UserPreferenceEntity
│   ├── criteria/              # SearchCriteriaService
│   ├── matching/              # JobMatchingService (cold + AI filter pipeline)
│   └── common/                # Shared: NotFoundException, ValueMappedEnum, utils
└── infrastructure/            # Technical concerns
    ├── ai/                    # ClaudeClient, AiConfig, AiProperties
    ├── security/              # SecurityConfig, Auth0Properties
    └── config/                # OpenApi, Clock, JpaAuditing, Scheduling, Web
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/jobs/ingest` | Batch ingest jobs from n8n |
| `GET` | `/jobs` | User's matched jobs (with optional status filter) |
| `PATCH` | `/jobs/{id}/status` | Update job status (NEW/APPLIED/IRRELEVANT) |
| `GET` | `/criteria?source={SOURCE}` | Aggregated search criteria for n8n |
| `GET` | `/preferences` | Get user preferences |
| `PUT` | `/preferences` | Save user preferences |
| `POST` | `/preferences/normalize` | AI-normalize raw text to structured preferences |
| `GET` | `/actuator/health` | Health check |

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5440` | PostgreSQL port |
| `DB_NAME` | `jobhunter` | Database name |
| `DB_USERNAME` | `jobhunter` | Database user |
| `DB_PASSWORD` | `jobhunter` | Database password |
| `SERVER_PORT` | `8080` (`8095` in local profile) | Application port |
| `AUTH0_ENABLED` | `true` | Enable/disable Auth0 |
| `AUTH0_ISSUER` | - | Auth0 issuer URL |
| `AUTH0_AUDIENCE` | - | Auth0 audience |
| `AI_ENABLED` | `false` | Enable Claude AI filtering |
| `ANTHROPIC_AUTH_TOKEN` | - | OAuth token from `claude setup-token` |

## License

MIT
