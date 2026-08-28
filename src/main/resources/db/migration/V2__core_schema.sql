-- Catalog, users, bookings, waitlist, refresh tokens.
-- Overlap: exclusion constraints apply only to PENDING and CONFIRMED.

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_role_check CHECK (role IN ('GUEST', 'STAFF'))
);

CREATE TABLE club_tables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    capacity SMALLINT NOT NULL,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT club_tables_capacity_check CHECK (capacity BETWEEN 2 AND 8)
);

CREATE TABLE games (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(200) NOT NULL,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE game_copies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id UUID NOT NULL REFERENCES games (id),
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX game_copies_game_id_idx ON game_copies (game_id);

CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    table_id UUID NOT NULL REFERENCES club_tables (id),
    game_copy_id UUID NOT NULL REFERENCES game_copies (id),
    user_id UUID NOT NULL REFERENCES users (id),
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    guest_count SMALLINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    idempotency_key VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL REFERENCES users (id),
    CONSTRAINT bookings_status_check CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT bookings_guest_count_check CHECK (guest_count BETWEEN 1 AND 8),
    CONSTRAINT bookings_interval_check CHECK (end_at > start_at)
);

CREATE INDEX bookings_user_id_idx ON bookings (user_id);
CREATE INDEX bookings_table_id_idx ON bookings (table_id);
CREATE INDEX bookings_game_copy_id_idx ON bookings (game_copy_id);
CREATE INDEX bookings_created_by_idx ON bookings (created_by);

CREATE UNIQUE INDEX bookings_user_idempotency_key_uidx
    ON bookings (user_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

ALTER TABLE bookings ADD CONSTRAINT bookings_no_overlap_table
    EXCLUDE USING gist (
        table_id WITH =,
        tstzrange(start_at, end_at, '[)') WITH &&
    )
    WHERE (status IN ('PENDING', 'CONFIRMED'));

ALTER TABLE bookings ADD CONSTRAINT bookings_no_overlap_copy
    EXCLUDE USING gist (
        game_copy_id WITH =,
        tstzrange(start_at, end_at, '[)') WITH &&
    )
    WHERE (status IN ('PENDING', 'CONFIRMED'));

CREATE TABLE waitlist_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id),
    table_id UUID REFERENCES club_tables (id),
    game_copy_id UUID REFERENCES game_copies (id),
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT waitlist_status_check CHECK (status IN ('ACTIVE', 'FULFILLED', 'CANCELLED')),
    CONSTRAINT waitlist_target_check CHECK (table_id IS NOT NULL OR game_copy_id IS NOT NULL),
    CONSTRAINT waitlist_interval_check CHECK (end_at > start_at)
);

CREATE INDEX waitlist_entries_user_id_idx ON waitlist_entries (user_id);
CREATE INDEX waitlist_entries_table_id_idx ON waitlist_entries (table_id);
CREATE INDEX waitlist_entries_game_copy_id_idx ON waitlist_entries (game_copy_id);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT refresh_tokens_token_hash_unique UNIQUE (token_hash)
);

CREATE INDEX refresh_tokens_user_id_idx ON refresh_tokens (user_id);
