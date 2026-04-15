-- Phase 2: API Key System
-- Supports dual key types: USER keys (self-service) and PARTNER keys (admin-managed).
-- Keys are never stored in plain text; only a SHA-256 hex digest is kept.
-- key_prefix stores the first 15 chars of the plain key for display in list endpoints.

CREATE TABLE api_keys (
    id                UUID          PRIMARY KEY,
    user_id           UUID          REFERENCES users(id) ON DELETE SET NULL,
    name              VARCHAR(255)  NOT NULL,
    key_prefix        VARCHAR(20)   NOT NULL,
    hashed_key        VARCHAR(64)   NOT NULL UNIQUE,
    key_type          VARCHAR(20)   NOT NULL,             -- 'USER' | 'PARTNER'
    scope             VARCHAR(50)   NOT NULL,             -- 'READ' | 'WRITE'
    partner_name      VARCHAR(255),
    partner_email     VARCHAR(255),
    rate_limit_read   INT           NOT NULL DEFAULT 20,
    rate_limit_write  INT           NOT NULL DEFAULT 10,
    redemption_code   VARCHAR(50)   UNIQUE,
    redemption_limit  INT,
    redemption_count  INT           NOT NULL DEFAULT 0,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by        UUID          REFERENCES users(id),
    last_used_at      TIMESTAMP WITH TIME ZONE,
    usage_count       BIGINT        NOT NULL DEFAULT 0,
    is_active         BOOLEAN       NOT NULL DEFAULT TRUE,
    revoked_at        TIMESTAMP WITH TIME ZONE,
    revoked_by        UUID          REFERENCES users(id),
    notes             TEXT
);

-- Fast lookup by key_type for admin list endpoints.
CREATE INDEX idx_api_keys_key_type ON api_keys (key_type);

-- Partial index on redemption_code: only rows that actually have a code are indexed,
-- keeping the index small while still allowing efficient redemption lookups.
CREATE INDEX idx_api_keys_redemption_code ON api_keys (redemption_code)
    WHERE redemption_code IS NOT NULL;

-- Partial index on partner_name for partner key filtering (skips user keys).
CREATE INDEX idx_api_keys_partner_name ON api_keys (partner_name)
    WHERE partner_name IS NOT NULL;

-- Fast lookup of all keys belonging to a user.
CREATE INDEX idx_api_keys_user_id ON api_keys (user_id)
    WHERE user_id IS NOT NULL;
