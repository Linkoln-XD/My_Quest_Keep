# Data

## Migrations

| File | What |
|---|---|
| `V1__enable_btree_gist.sql` | `CREATE EXTENSION btree_gist` (needed for exclusion + UUID equality) |
| `V2__core_schema.sql` | All application tables and constraints |

Location: `src/main/resources/db/migration`. Hibernate `ddl-auto=validate` does not change schema.

**Reproduce:** empty Postgres 16 → start the app (or `./mvnw test` with Testcontainers). Flyway applies V1 then V2. To reset Compose: `docker compose down -v` then `up`.

## Tables

### `users`

`id` UUID PK, `email` unique (stored lowercased), `password_hash` (BCrypt), `role` `GUEST`/`STAFF`, `created_at`.

### `club_tables`

Furniture. `name`, `capacity` SMALLINT 2–8 (JPA `int` mapped as smallint), `deleted_at` nullable, `created_at`, `updated_at`.

### `games` / `game_copies`

`games.title`, soft-delete. Copies: `game_id` FK, soft-delete. Index on `game_id`.

### `bookings`

`table_id`, `game_copy_id`, `user_id`, `start_at`, `end_at`, `guest_count` SMALLINT 1–8 (JPA `int` mapped as smallint), `status`, `idempotency_key`, `created_at`, `updated_at`, `created_by`.

- Check `end_at > start_at`.
- Partial unique `(user_id, idempotency_key)` where key is not null.
- **EXCLUDE gist** on table id + `tstzrange(start_at,end_at,'[)')` where status in (`PENDING`,`CONFIRMED`).
- Same for `game_copy_id`.

### `waitlist_entries`

`user_id`, nullable `table_id` / `game_copy_id` (at least one required), interval, `status` `ACTIVE`/`FULFILLED`/`CANCELLED`.

### `refresh_tokens`

`user_id` ON DELETE CASCADE, `token_hash` SHA-256 hex unique (64 chars), `expires_at`, `revoked_at`, `created_at`. Raw refresh is never stored.

## Indexes

PK plus FK indexes on copies, bookings (`user_id`, `table_id`, `game_copy_id`, `created_by`), waitlist FKs, `refresh_tokens.user_id`.
