# Architecture decisions / Решения

Format: date, options, choice, why. Both languages.

## 2026-08-28 — Spring Boot version

- Options: keep repo 4.1.1 / downgrade to 3.x as in the original brief.
- Choice: **Spring Boot 4.1.1**, Java 21.
- Why: already in the generated `pom.xml`; confirmed by product owner.

## 2026-08-28 — Time slots and overlap

- Interval: half-open **`[start, end)`**, `timestamptz` **UTC** in PostgreSQL; clients send ISO-8601 with offset.
- Grid: 30-minute alignment; duration 1–4 hours; **future only**.
- Overlap: PostgreSQL **exclusion constraint + `btree_gist`**, plus a service transaction.
- Why: correct under concurrent requests; portfolio-relevant.

## 2026-08-28 — Auth and users

- Public `POST /api/v1/auth/register` → role **GUEST**; **STAFF** via seed.
- Users table (`id`, `email`, password hash, `role`, `created_at`).
- JWT: **access 15 min + refresh 7 days** (refresh stored in DB). Implementation in a later step.
- Why: owner asked to pick a practical JWT shape; table is required for a portfolio.

## 2026-08-28 — API and packaging

- Packages: **by feature** under `ru.link.questkeep`.
- Mapping: **manual** (no MapStruct).
- Errors: RFC 7807 / 9457 Problem Details (`spring.mvc.problemdetails.enabled`).
- Booking conflict: **409**.
- Create booking: **Idempotency-Key**.
- Pagination: `page`/`size`, 0-based, max 100.
- HTTP samples: **`http/*.http` only** (no Postman).
- Compose: `questkeep-app`, `questkeep-db`, **Adminer** `questkeep-adminer`; app port **8080**.
- Tests: JUnit 5 + MockMvc / `@SpringBootTest` + **Testcontainers** PostgreSQL.

## 2026-08-28 — Domain rules (to implement in later steps)

- Statuses: PENDING / CONFIRMED / CANCELLED / EXPIRED.
- Guest create → **CONFIRMED** immediately; **PENDING** reserved for later flows (not used on the public happy path).
- **EXPIRED** applied **on read** when `end <= now` and status was PENDING or CONFIRMED.
- Waitlist on CONFIRMED cancel: **DB only**, no email/queue; staff can list entries.
- Catalog: **soft-delete**; refuse delete while active bookings exist (details in domain step).
- Booking audit: `created_at`, `updated_at`, `created_by`.

## 2026-08-28 — Schema (step 4)

- Physical table for club tables: **`club_tables`** (avoids SQL `TABLE` confusion). API name remains “table”.
- Waitlist row statuses: **ACTIVE / FULFILLED / CANCELLED** (staff listing later). A row must reference a table and/or a game copy.
- **`refresh_tokens`** in `V2` (hash only, not the raw token). JWT wiring still later.
- FK indexes on booking/waitlist/copy FKs; unique `(user_id, idempotency_key)` where key is present.
- Soft-delete of catalog with active bookings: **blocked in the service** (step 5/7), not a DB trigger.
- Adjacent slots do not overlap: `[12:00,14:00)` and `[14:00,16:00)` are allowed.
