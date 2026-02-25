# Job Hunter API

Kotlin Spring Boot backend for job vacancy monitoring and tracking. Part of the [Job Hunter](https://github.com/mshykhov/job-hunter) system.

## Overview

REST API that receives scraped job listings from n8n workflows, stores them in PostgreSQL, and provides aggregated search criteria for scraping configuration.

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Kotlin | 2.1 | Language |
| Spring Boot | 3.5 | Framework |
| Spring Data JPA | - | Database access |
| PostgreSQL | 16 | Database |
| Flyway | - | Schema migrations |
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
├── config/              # Spring configuration
├── controller/
│   ├── job/             # Job endpoints + DTOs
│   └── criteria/        # Search criteria endpoints + DTOs
├── service/             # Business logic
├── persistence/
│   ├── model/           # JPA entities, enums
│   ├── repository/      # Spring Data JPA interfaces
│   └── facade/          # Transactional facades
└── exception/           # Error handling, exceptions
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/jobs/ingest` | Batch ingest jobs from n8n |
| `GET` | `/criteria?source={SOURCE}` | Aggregated search criteria for n8n |
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

## License

MIT
