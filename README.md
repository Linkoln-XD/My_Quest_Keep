# QuestKeep

EN: REST API for booking a **table** and a **specific game copy** together at a tabletop club.

RU: REST API для одновременной брони **стола** и **конкретной копии игры** в настольном клубе.

Status: **API (step 6)** — JWT auth, catalog, bookings. Waitlist staff UX is next.

Swagger: http://localhost:8080/swagger-ui.html (Authorize with Bearer access token).

Demo STAFF (seed): `staff@questkeep.local` / `ChangeMe_Staff_Demo_1` (see `.env.example`).

HTTP samples: [`http/auth.http`](http/auth.http), [`http/catalog.http`](http/catalog.http), [`http/bookings.http`](http/bookings.http).

## Stack

- Java 21, Spring Boot **4.1.1**, Maven
- Spring Web MVC, Data JPA, Validation, Security (permit-all until JWT)
- PostgreSQL 16, Flyway
- springdoc-openapi / Swagger UI
- Tests: JUnit 5, Testcontainers

## Quick start (Docker Compose)

Requires Docker. From the repository root:

```bash
cp .env.example .env
docker compose up --build
```

| Service | URL |
|---|---|
| API | http://localhost:8080 |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Adminer | http://localhost:8081 |

Adminer: system **PostgreSQL**, server **`db`**, username/password/database from `.env` (defaults: `questkeep` / `questkeep` / `questkeep`).

Stop: `docker compose down`. Data volume: `questkeep-db-data`.

## Local run (IDE / Maven)

1. Start only the database: `docker compose up db adminer`
2. `cp .env.example .env` and keep `DATABASE_URL=jdbc:postgresql://localhost:5432/questkeep`
3. `./mvnw spring-boot:run`

## Tests

Docker is required (Testcontainers PostgreSQL):

```bash
./mvnw test
```

## HTTP samples

See [`http/openapi.http`](http/openapi.http) (IntelliJ / VS Code REST Client).

## Environment

See [`.env.example`](.env.example). Do not commit `.env`. Passwords in examples are **demo-only**.

| Variable | Meaning |
|---|---|
| `SERVER_PORT` | Host port for the API (Compose) |
| `POSTGRES_*` | Postgres container |
| `DATABASE_URL` / `USERNAME` / `PASSWORD` | JDBC for the app (local Maven uses `localhost`) |
| `ADMINER_PORT` | Host port for Adminer |
| `LOGGING_STRUCTURED_FORMAT` | `ecs` (default), `logstash`, `gelf`, or empty |

## Layout

Packages are **by feature** under `ru.link.questkeep` (domain packages come in later steps). Shared config: `shared.config`.

Flyway: `src/main/resources/db/migration`. `V1` enables `btree_gist`. `V2` creates users, catalog, bookings, waitlist, refresh tokens.

## Docs

- [docs/DECISIONS.md](docs/DECISIONS.md) — agreed choices
- Full README / ARCHITECTURE / API / DOMAIN (RU+EN) after the API exists

## MVP limits (current)

No waitlist API yet. CORS allows only localhost / 127.0.0.1.
