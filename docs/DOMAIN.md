# Domain

## Booking rules / Правила брони

**EN / RU**

- One booking = one **table** + one **game copy** + interval + guest count.
- Interval is half-open **`[start, end)`** in UTC (`timestamptz`). Clients send ISO-8601 with offset; the DB stores UTC.
- Start and end must sit on a **30-minute** UTC grid (`:00` or `:30`, seconds/nanos 0).
- Duration **1–4 hours**, multiple of 30 minutes.
- Start must be **strictly after** “now” (application `Clock`, UTC).
- Guest count ≥ 1 and **≤ table capacity** (capacity 2–8).
- Table, copy, and game must not be soft-deleted.

## Overlap / Пересечение

Two intervals overlap iff `startA < endB AND startB < endA` (same as `tstzrange(..., '[)') &&`).

`[12:00,14:00)` and `[14:00,16:00)` do **not** overlap.

Active occupancy: status **PENDING** or **CONFIRMED**. PostgreSQL exclusion constraints `bookings_no_overlap_table` and `bookings_no_overlap_copy` enforce this. Concurrent inserts may deadlock (`40P01`); the service retries, then maps remaining exclusion to conflict.

## Statuses / Статусы брони

| Status | Meaning |
|---|---|
| `CONFIRMED` | Set **immediately** on create (public API). |
| `PENDING` | Allowed in DB; **not** assigned by guest create. |
| `CANCELLED` | Cancel; repeating cancel is a no-op. |
| `EXPIRED` | On **read** (`get` / lists) if `now >= end` and status was PENDING or CONFIRMED. Cannot cancel EXPIRED. |

## Roles

| Role | Access |
|---|---|
| `GUEST` | Register publicly. Own bookings and waitlist. Catalog **read**. |
| `STAFF` | Seeded user. Catalog write, all bookings, all **ACTIVE** waitlist, cancel any booking/waitlist row they can see. |

## Waitlist

A row targets a **table and/or a copy** plus an interval (same slot rules as bookings). Statuses: `ACTIVE` (listed for STAFF), `CANCELLED`, `FULFILLED` (entity only; API does not auto-fulfill). Duplicate ACTIVE (same user, targets, interval) is reused. No email when a CONFIRMED booking is cancelled.

## Catalog delete

Soft-delete (`deleted_at`). Refused if there is a PENDING/CONFIRMED booking with `end > now` on that table, copy, or (for a game) any copy of that game.
