-- Phase 2: Refresh token rotation + password version tracking
-- NOTE: V6 is used for Phase 2 because Phase 1 (Q&A) was deprioritized.
-- When Phase 1 is implemented, its migration must use V7 or higher.
-- Flyway's outOfOrder mode is NOT required since no prior V6 exists.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_version INT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    -- SHA-256 hex digest of the opaque raw token sent to the client.
    -- The raw token is never stored; only its hash is persisted.
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    issued_at   TIMESTAMP   NOT NULL DEFAULT now(),
    expires_at  TIMESTAMP   NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE,
    user_agent  VARCHAR(500),
    ip_address  VARCHAR(45)
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user   ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_hash   ON refresh_tokens(token_hash);
