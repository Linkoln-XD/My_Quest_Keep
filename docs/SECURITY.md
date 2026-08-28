# Security

## In scope (pet project)

- Passwords: BCrypt. Demo STAFF password lives in `.env.example` on purpose; change it.
- Access JWT: HS256, `sub` = user id, claims `email` and `role`, TTL 15 minutes. Secret from `JWT_SECRET` (≥ 32 bytes).
- Refresh: random 32 bytes (URL-safe Base64), SHA-256 hex in DB, TTL 7 days, **rotated** on `/auth/refresh` (old row revoked).
- HTTP: CSRF off (stateless Bearer). Sessions off.
- CORS: `http://localhost:*` and `http://127.0.0.1:*` only.
- Logs: structured ECS by default; do not log tokens or passwords. Invalid JWT is answered as 401 without echoing the token.
- GUEST cannot learn another user’s booking/waitlist id: wrong-owner cancel/get → **404**.
- Catalog writes and `GET /api/v1/bookings` (all) and `GET /api/v1/waitlist` (active) require `STAFF`.

## Threats we accept / accept as residual

**EN:** Stolen demo JWT secret, brute-force login (no rate limit), XSS if the demo UI is sloppy (tokens in sessionStorage), Adminer exposed on localhost, no HTTPS in Compose, no account lockout, seed STAFF created if email missing.

**RU:** Украденный демо-секрет JWT, перебор пароля без лимита, XSS в демо-UI (токены в sessionStorage), Adminer на localhost, нет TLS в Compose, нет блокировки аккаунта.

## Consciously not done

OAuth2/OIDC, email verification, password reset, refresh-token reuse detection beyond revoke-on-rotate, WAF, GitHub Actions security scanning, row-level security in Postgres, audit log table.
