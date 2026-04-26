ALTER TABLE reports ADD COLUMN updated_at TIMESTAMP;
UPDATE reports SET updated_at = created_at WHERE updated_at IS NULL;
