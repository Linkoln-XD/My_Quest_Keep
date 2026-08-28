# API

Machine-readable contract: http://localhost:8080/v3/api-docs · UI: `/swagger-ui.html`. Demo SPA (Compose): http://localhost:8080.

Base path: `/api/v1`. JSON. Errors: Problem Details (`application/problem+json` when the handler runs): `type`, `title`, `status`, `detail`.

Pagination: `page` (0-based), `size` (1–100, default 20). Response: `content`, `page`, `size`, `totalElements`, `totalPages`.

Auth header: `Authorization: Bearer <accessToken>`.

---

## Auth

| Method | Path | Auth |
|---|---|---|
| POST | `/auth/register` | public → GUEST, **201** |
| POST | `/auth/login` | public, **200** |
| POST | `/auth/refresh` | public, body `{ "refreshToken" }` |

Register/login/refresh body (register/login): `{ "email", "password" }` (register: password min 8).  
Response: `{ "accessToken", "refreshToken", "tokenType": "Bearer" }`.

| Status | When |
|---|---|
| 400 | Validation / email already registered |
| 401 | Bad password or dead refresh token |

---

## Catalog

**GET** (any authenticated user): tables, games, copies.  
**POST/PATCH/DELETE** (STAFF). Soft-delete → **204**. Delete blocked if an active booking exists → **400**.

| Method | Path |
|---|---|
| POST/GET | `/tables` |
| GET/PATCH/DELETE | `/tables/{id}` |
| POST/GET | `/games` |
| GET/PATCH/DELETE | `/games/{id}` |
| POST/GET | `/games/{gameId}/copies` |
| GET/DELETE | `/game-copies/{id}` |

Create table: `{ "name", "capacity": 2-8 }`. Create game: `{ "title" }`. Copies have no body.

---

## Bookings

**POST `/bookings`** — GUEST or STAFF (as themselves). **Required header** `Idempotency-Key` (max 128). Body:

```json
{
  "tableId": "uuid",
  "gameCopyId": "uuid",
  "startAt": "2026-09-01T12:00:00Z",
  "endAt": "2026-09-01T14:00:00Z",
  "guestCount": 2
}
```

**201** `CONFIRMED`. Same user + same key + same payload → same booking. Same key, different payload → **400**. Overlap on table or copy → **409**.

| Method | Path | Who |
|---|---|---|
| GET | `/bookings/me` | caller |
| GET | `/bookings` | STAFF all |
| GET | `/bookings/{id}` | owner or STAFF; other GUEST → **404** |
| POST | `/bookings/{id}/cancel` | owner or STAFF; other GUEST → **404**; idempotent |

---

## Waitlist

| Method | Path | Who |
|---|---|---|
| POST | `/waitlist` | authenticated. Body: `tableId` and/or `gameCopyId`, `startAt`, `endAt` |
| GET | `/waitlist/me` | caller |
| GET | `/waitlist` | STAFF, **ACTIVE** only, oldest first |
| POST | `/waitlist/{id}/cancel` | owner or STAFF; other GUEST → **404**; idempotent |

Duplicate ACTIVE join → same id (**201** again).

---

## Scenarios

### Book / забронировать

STAFF creates table + game + copy. GUEST registers, `POST /bookings` with `Idempotency-Key` → 201 `CONFIRMED`.

### Conflict / конфликт

Second GUEST, overlapping `[start,end)` on the **same table** (other copy) or **same copy** (other table) → **409**, first row unchanged.

### Cancel / отмена

`POST /bookings/{id}/cancel` → `CANCELLED`. Repeat → still `CANCELLED`. Waitlist is **not** emailed; STAFF uses `GET /waitlist`.

### Waitlist / лист ожидания

GUEST `POST /waitlist` with `tableId` (and/or copy) and the same slot shape as a booking. STAFF `GET /waitlist`. GUEST cannot `GET /waitlist` (403).

---

## Typical codes

| Code | Meaning |
|---|---|
| 400 | Domain/validation (capacity, slots, missing `Idempotency-Key`, catalog delete blocked) |
| 401 | No/invalid/expired access token |
| 403 | Authenticated but not STAFF (catalog write, list all bookings/waitlist) |
| 404 | Missing or hidden (other user’s booking/waitlist) |
| 409 | Booking overlap |
