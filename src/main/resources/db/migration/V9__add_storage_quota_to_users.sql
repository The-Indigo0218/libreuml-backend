-- V9__add_storage_quota_to_users.sql
-- Phase 1 / 01 — Per-user storage quota enforcement (10 MB = 10 485 760 bytes).
--
-- Backward-compatible: both columns declare NOT NULL DEFAULT so existing rows receive
-- quota = 10 MB and used = 0 (recomputable from diagram content sizes if needed).
-- New users inserted after this migration also get the DEFAULT values unless the
-- application layer provides explicit values.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS storage_quota_bytes BIGINT NOT NULL DEFAULT 10485760,
    ADD COLUMN IF NOT EXISTS storage_used_bytes  BIGINT NOT NULL DEFAULT 0;
