-- Project module: project-centric cloud sync.
-- A project owns exactly one semantic model and zero-or-more diagrams. All three live here in a
-- single migration because they ship as one feature. JSON payloads are stored as jsonb; optimistic
-- locking is handled by the application via the `version` column (@Version in JPA). The model and
-- diagram rows cascade-delete with their parent project.

CREATE TABLE projects (
    id              UUID         PRIMARY KEY,
    owner_id        UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    author          VARCHAR(255),
    project_version VARCHAR(50)  NOT NULL DEFAULT '1.0.0',
    project_kind    VARCHAR(50)  NOT NULL,
    target_language VARCHAR(100),
    base_package    VARCHAR(255),
    visibility      VARCHAR(50)  NOT NULL DEFAULT 'PRIVATE',
    vfs_snapshot    JSONB,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Fast lookup of all projects belonging to a user (list endpoint).
CREATE INDEX idx_projects_owner_id ON projects (owner_id);

-- The semantic model: one-to-one with a project, enforced by the UNIQUE constraint on project_id.
CREATE TABLE semantic_models (
    id          UUID   PRIMARY KEY,
    project_id  UUID   NOT NULL UNIQUE REFERENCES projects(id) ON DELETE CASCADE,
    data        JSONB  NOT NULL DEFAULT '{}',
    version     BIGINT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Diagrams scoped to a project. `path` stores the VFS node UUID assigned by the frontend.
CREATE TABLE project_diagrams (
    id           UUID         PRIMARY KEY,
    project_id   UUID         NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name         VARCHAR(255) NOT NULL,
    diagram_type VARCHAR(50)  NOT NULL,
    path         VARCHAR(255),
    view_data    JSONB        NOT NULL DEFAULT '{}',
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Fast lookup of all diagrams belonging to a project (list + full-project load).
CREATE INDEX idx_project_diagrams_project_id ON project_diagrams (project_id);

-- GIN index enables efficient containment (@>) and path queries on diagram view data.
CREATE INDEX idx_project_diagrams_view_data_gin ON project_diagrams USING GIN (view_data);
