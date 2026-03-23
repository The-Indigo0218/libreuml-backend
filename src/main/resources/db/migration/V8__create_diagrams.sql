-- Phase 4: Diagram Cloud Sync
-- Diagrams are the core domain entity. Content is stored as JSONB for flexible schema evolution.
-- GIN index on content enables path-based queries inside the JSON document.
-- Optimistic locking is handled by the application via the `version` column (@Version in JPA).

CREATE TABLE diagrams (
    id          UUID         PRIMARY KEY,
    owner_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    type        VARCHAR(50)  NOT NULL,
    visibility  VARCHAR(50)  NOT NULL DEFAULT 'PRIVATE',
    content     JSONB        NOT NULL DEFAULT '{}',
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Fast lookup of all diagrams belonging to a user (list endpoint).
CREATE INDEX idx_diagrams_owner_id ON diagrams (owner_id);

-- GIN index allows efficient containment (@>) and path (jsonb_path_ops) queries on diagram content.
CREATE INDEX idx_diagrams_content_gin ON diagrams USING GIN (content);

-- Join table for shared-diagram collaborators. Both sides cascade-delete to avoid orphaned rows.
CREATE TABLE diagram_collaborators (
    diagram_id UUID NOT NULL REFERENCES diagrams(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    PRIMARY KEY (diagram_id, user_id)
);
