# Job Hunter API

Kotlin Spring Boot backend for job vacancy monitoring and tracking. Part of the [Job Hunter](https://github.com/mshykhov/job-hunter) system.

## Overview

REST API that receives scraped job listings from n8n workflows, stores them in PostgreSQL, and provides endpoints for managing vacancies. Includes a Telegram bot for push notifications.

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Kotlin | 2.1 | Language |
| Spring Boot | 3.5 | Framework |
| Spring Data JPA | - | Database access |
| PostgreSQL | 16 | Database |
| Flyway | - | Schema migrations |
| SpringDoc OpenAPI | 2.8 | API documentation |
| Testcontainers | - | Integration testing |
| Docker | - | Containerization |
| Helm | - | Kubernetes deployment |

## Quick Start

```bash
# Start PostgreSQL
cp .env.example .env
docker compose up -d

# Run the application
./gradlew bootRun --args='--spring.profiles.active=local'
```

Open [http://localhost:8080/swagger-ui](http://localhost:8080/swagger-ui) for API docs.

## Project Structure

```
src/main/kotlin/com/mshykhov/jobhunter/
├── config/              # Spring configuration
├── domain/
│   ├── model/           # JPA entities, enums
│   └── repository/      # Spring Data repositories
├── application/
│   └── service/         # Business logic
├── infrastructure/
│   ├── persistence/     # JPA-specific implementations
│   └── telegram/        # Telegram bot integration
└── web/
    ├── controller/      # REST controllers
    └── dto/             # Request/Response objects
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/jobs/ingest` | Receive jobs from n8n |
| `GET` | `/api/jobs` | List jobs with filters |
| `PATCH` | `/api/jobs/{id}/status` | Update job status |
| `GET` | `/actuator/health` | Health check |

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5433` | PostgreSQL port |
| `DB_NAME` | `jobhunter` | Database name |
| `DB_USERNAME` | `jobhunter` | Database user |
| `DB_PASSWORD` | - | Database password |
| `SERVER_PORT` | `8080` | Application port |

## License

MIT
