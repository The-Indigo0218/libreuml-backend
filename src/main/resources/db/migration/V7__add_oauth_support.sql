-- Phase 3: OAuth provider identity columns
-- Both columns are nullable because existing local-auth users have no OAuth provider IDs.
-- UNIQUE constraints prevent two accounts from sharing the same provider identity.
-- Partial indexes on non-null values keep index size small and queries fast.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS github_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS google_id VARCHAR(255);

ALTER TABLE users
    ADD CONSTRAINT uq_users_github_id UNIQUE (github_id),
    ADD CONSTRAINT uq_users_google_id UNIQUE (google_id);

CREATE INDEX IF NOT EXISTS idx_users_github_id ON users (github_id) WHERE github_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_google_id ON users (google_id) WHERE google_id IS NOT NULL;
