# QuestKeep

[![CI](https://github.com/Linkoln-XD/My_Quest_Keep/actions/workflows/ci.yml/badge.svg)](https://github.com/Linkoln-XD/My_Quest_Keep/actions/workflows/ci.yml)

**EN:** REST API for a tabletop club: a guest books a **table** and a **specific game copy** in one reservation.

**RU:** REST API настольного клуба: гость бронирует **стол** и **конкретную копию игры** одной бронью.

| | |
|---|---|
| Stack | Java 21, Spring Boot 4.1.1, PostgreSQL 16, Flyway, Maven; React (Vite) demo UI |
| Auth | JWT access 15 min + refresh 7 days; public guest register; STAFF seed |
| Docs | [ARCHITECTURE](docs/ARCHITECTURE.md) · [API](docs/API.md) · [DOMAIN](docs/DOMAIN.md) · [DATA](docs/DATA.md) · [SECURITY](docs/SECURITY.md) · [DECISIONS](docs/DECISIONS.md) |

## 10-minute start / Запуск за 10 минут

Needs **Docker Desktop running**. From the repo root:

```bash
cp .env.example .env
docker compose up --build
```

Wait until `questkeep-web` is Up. Open **http://localhost:8080** — that is the React UI. `/api` and Swagger go through the same port (nginx → Spring). Two guests = two tabs (`sessionStorage`).

**EN:** If Compose stays in `Created` and never becomes `healthy`/`Up`, port **5432** is almost always already bound (another Postgres container or a local server). The default in `.env.example` maps Postgres to **5433** on the host. App ↔ DB inside Compose still uses hostname `db` and port **5432**.

**RU:** Если контейнеры зависают в `Created`, чаще всего занят хостовый **5432**. По умолчанию Postgres пробрасывается на **5433**. Между сервисами Compose БД доступна как `db:5432`.

| Service | URL |
|---|---|
| Demo UI + API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Adminer | http://localhost:8081 |

Adminer: system **PostgreSQL**, server **`db`**, user/password/database from `.env` (default `questkeep` / `questkeep` / `questkeep`).

**EN:** In Swagger click **Authorize** and paste `Bearer <accessToken>` (or the token alone if the UI adds the scheme).  
**RU:** В Swagger — **Authorize**, вставьте access-токен.

Demo STAFF (seed on first start): `staff@questkeep.local` / `ChangeMe_Staff_Demo_1`. Change these in `.env` before any real use.

Stop: `docker compose down`. Volume: `questkeep-db-data` (use `docker compose down -v` to wipe the DB).

## Local Maven (without building the app image)

Needs **JDK 21** (`java -version`). A newer JDK (e.g. 26) is not the image we ship; prefer Temurin 21 or run the app via Compose.

```bash
cp .env.example .env
docker compose up db adminer
./mvnw spring-boot:run
```

Spring Boot does **not** auto-load `.env`. Defaults in `application.properties` match `.env.example` (`localhost:5433`). If you change `POSTGRES_PORT`, set `DATABASE_URL` in the IDE run config (or export it) to the same host port. Do not use hostname `db` outside Compose — it exists only on the Docker network.

## Demo UI (React)

Compose: **http://localhost:8080** (nginx serves the SPA and proxies `/api`).

Hot reload without the `web` container — API on 8080 via Maven, then:

```bash
cd frontend
npm ci
npm run dev
```

Vite is http://localhost:5173 and proxies `/api` to `http://127.0.0.1:8080`. STAFF: `staff@questkeep.local` / `ChangeMe_Staff_Demo_1`.

On the home screen: log in as staff → **Завести Oak + Catan + копию** → register a guest in another tab → book the same slot twice to see **409**.

## Tests

Docker is required (Testcontainers PostgreSQL 16). Same command locally and in CI:

```bash
./mvnw test
```

**CI:** GitHub Actions (`.github/workflows/ci.yml`) runs `./mvnw -B verify` and `frontend` `npm ci && npm run build` on every push to `main`, every pull request, and on manual **Run workflow**.

**EN:** Host JDK must be 21 if you run Maven outside Compose. CI always uses 21.

**RU:** Локально для Maven нужен JDK 21; в CI он фиксирован.

## curl examples

Register a guest and book (replace IDs from catalog after STAFF creates a table and a copy):

```bash
# Login STAFF
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"staff@questkeep.local","password":"ChangeMe_Staff_Demo_1"}'

# Register GUEST
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"guest@example.com","password":"password1"}'

# Create booking (GUEST access token)
curl -s -X POST http://localhost:8080/api/v1/bookings \
  -H 'Authorization: Bearer ACCESS_TOKEN' \
  -H 'Idempotency-Key: demo-1' \
  -H 'Content-Type: application/json' \
  -d '{"tableId":"TABLE_UUID","gameCopyId":"COPY_UUID","startAt":"2026-09-01T12:00:00Z","endAt":"2026-09-01T14:00:00Z","guestCount":2}'
```

More requests: [`http/auth.http`](http/auth.http), [`http/catalog.http`](http/catalog.http), [`http/bookings.http`](http/bookings.http), [`http/waitlist.http`](http/waitlist.http).

## Environment

Copy [`.env.example`](.env.example) to `.env`. Do not commit `.env`. Values are **demo-only**.

| Variable | Meaning |
|---|---|
| `SERVER_PORT` | Public Compose URL: React UI + `/api` (nginx, default 8080) |
| `POSTGRES_DB` / `USER` / `PASSWORD` / `PORT` | Postgres; `PORT` is the **host** mapping (default 5433) |
| `DATABASE_URL` / `USERNAME` / `PASSWORD` | JDBC for local Maven (`localhost` + `POSTGRES_PORT`) |
| `ADMINER_PORT` | Adminer host port |
| `LOGGING_STRUCTURED_FORMAT` | `ecs` (default), `logstash`, `gelf`, or empty |
| `JWT_SECRET` | HS256 secret, at least 32 bytes |
| `STAFF_EMAIL` / `STAFF_PASSWORD` | Seeded STAFF if that email is not in the DB |

## Layout

Packages **by feature** under `ru.link.questkeep`: `identity`, `catalog`, `booking`, `shared`.

Flyway: `src/main/resources/db/migration` (`V1` `btree_gist`, `V2` core tables).

## If it does not start / Если не поднимается

```bash
docker compose ps -a
docker compose logs db
docker compose logs app
```

| Symptom | Cause | What to do |
|---|---|---|
| `db` stays `Created` / `Bind for 0.0.0.0:5432 failed` | Port 5432 already used | Keep `POSTGRES_PORT=5433` (default). Recreate: `docker compose down && docker compose up --build` |
| `app` cannot connect to Postgres | Local Maven using `db` as host, or wrong port | Inside Compose the URL is `jdbc:postgresql://db:5432/...`. On the host use `localhost` + `POSTGRES_PORT` |
| `Schema validation: wrong column type` (`smallint` vs `integer`) | JPA `int` vs Postgres `SMALLINT` | Already mapped in entities (`@JdbcTypeCode(SMALLINT)`). Rebuild the app image: `docker compose up --build` |
| `Unsupported class file major version` / Maven fails on the host | JDK is not 21 | Install Temurin 21, or run **only** `docker compose up --build` (image is JDK 21) |
| Docker socket / `Cannot connect to the Docker daemon` | Docker Desktop not running | Start Docker Desktop, wait until it is ready, retry |

## MVP limits / Ограничения MVP

**EN:** No payments, discounts, email, or queues. Booking create is immediately `CONFIRMED` (no STAFF confirm). Waitlist is stored only; cancel does not notify. `EXPIRED` is applied on read. CORS is localhost only. `PENDING` and waitlist `FULFILLED` are not used on the public happy path. The React UI is a demo client, not a production spa (no i18n, no design system).

**RU:** Нет оплаты, скидок, почты и очередей. Бронь сразу `CONFIRMED`. Лист ожидания только в БД, отмена никого не уведомляет. `EXPIRED` ставится при чтении. CORS только localhost. `PENDING` и `FULFILLED` на публичном happy-path не используются. React — демо-клиент, не прод.
