# Architecture

## Context (C4 level 1)

**EN:** QuestKeep is a single Spring Boot process plus a **demo React UI**. Guests and staff call HTTP JSON. PostgreSQL stores users, catalog, bookings, waitlist, and refresh-token hashes. Adminer is optional for inspecting the DB.

**RU:** Один процесс Spring Boot и демо-SPA на React. Гости и персонал ходят в HTTP JSON. PostgreSQL хранит пользователей, каталог, брони, лист ожидания и хеши refresh. Adminer — опционально.

```
[Guest / Staff — React demo :8080] --nginx /api--> [QuestKeep API inside Compose] ---- [PostgreSQL :5432 in Compose network]
        |                                                         (host mapping: POSTGRES_PORT, default 5433)
        +---- [Adminer :8081] (Compose only, talks to Postgres as host `db`)
```

## Containers (C4 level 2)

Compose names: `questkeep-app`, `questkeep-db`, `questkeep-adminer`, `questkeep-web` (React demo). Host **8080** is nginx: SPA + proxy to the API. Java is not published on the host.

The React app in `frontend/` talks only to `/api/v1`. Vite `npm run dev` proxies to `localhost:8080` (use Maven API, not the `web` container). Tokens stay in **sessionStorage** (one person per browser tab).

The app runs Flyway on startup, then Hibernate `validate`. JVM 21.

CI (GitHub Actions) does not start Compose. It runs `./mvnw verify` on Temurin 21; tests boot PostgreSQL **16** via Testcontainers (`postgres:16-alpine`).

## Application layers

Packages are **by feature**, not by technical layer.

| Package | Role |
|---|---|
| `identity` | Users, JWT, refresh tokens, STAFF seed, `/api/v1/auth` |
| `catalog` | Tables, games, copies, soft-delete |
| `booking` | Bookings, waitlist, overlap use-cases |
| `shared` | Security filter, Problem Details, pagination, Clock |

Controllers stay thin: validate HTTP, call a service, map to records by hand (no MapStruct).

Invariants live on entities (`Booking.confirmNew`, `TimeSlotRules`) and in services (`BookingService`, `CatalogService`, `WaitlistService`). Insert of a booking uses `BookingWriter` with `REQUIRES_NEW` so a PostgreSQL exclusion/unique error does not poison the outer persistence context.

## Main entities

See [DOMAIN.md](DOMAIN.md). Club furniture is table `club_tables` (API still says “table”). A booking always references one table **and** one `game_copies` row.

## Trust boundary

JWT Bearer on `/api/v1/**` except `/api/v1/auth/**` and OpenAPI/Swagger. Details: [SECURITY.md](SECURITY.md).
