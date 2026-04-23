-- Rename existing diagrams table to legacy_diagrams to preserve data during migration
ALTER TABLE diagrams RENAME TO legacy_diagrams;
ALTER TABLE diagram_collaborators RENAME TO legacy_diagram_collaborators;

ALTER INDEX idx_diagrams_owner_id RENAME TO idx_legacy_diagrams_owner_id;
ALTER INDEX idx_diagrams_content_gin RENAME TO idx_legacy_diagrams_content_gin;

-- Projects table
CREATE TABLE projects (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name             VARCHAR(255) NOT NULL,
    description      TEXT,
    author           VARCHAR(255),
    project_version  VARCHAR(50)  NOT NULL DEFAULT '1.0.0',
    target_language  VARCHAR(50),
    base_package     VARCHAR(255),
    visibility       VARCHAR(20)  NOT NULL DEFAULT 'PRIVATE'
                                  CHECK (visibility IN ('PRIVATE', 'SHARED', 'PUBLIC')),
    version          BIGINT       NOT NULL DEFAULT 1,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_projects_owner_id ON projects(owner_id);
CREATE INDEX idx_projects_updated_at ON projects(owner_id, updated_at DESC);

-- Project models table (one per project, SSOT for all UML elements)
CREATE TABLE project_models (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id   UUID        NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    model_data   JSONB       NOT NULL DEFAULT '{}',
    version      BIGINT      NOT NULL DEFAULT 1,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_project_models_project UNIQUE (project_id)
);

CREATE INDEX idx_project_models_project_id ON project_models(project_id);
CREATE INDEX idx_project_models_data ON project_models USING GIN (model_data);

-- New diagrams table (canvas view per diagram — no UML element definitions)
CREATE TABLE diagrams (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id   UUID         NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name         VARCHAR(255) NOT NULL,
    diagram_type VARCHAR(50)  NOT NULL
                              CHECK (diagram_type IN (
                                  'CLASS', 'USE_CASE', 'SEQUENCE', 'ACTIVITY',
                                  'STATE', 'COMPONENT', 'DEPLOYMENT', 'PACKAGE',
                                  'OBJECT', 'UNSPECIFIED'
                              )),
    path         VARCHAR(500),
    view_data    JSONB        NOT NULL DEFAULT '{}',
    version      BIGINT       NOT NULL DEFAULT 1,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_diagrams_project_id ON diagrams(project_id);
CREATE INDEX idx_diagrams_type ON diagrams(project_id, diagram_type);

-- Auto-update updated_at trigger function (shared across all three tables)
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_projects_updated_at
    BEFORE UPDATE ON projects
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_project_models_updated_at
    BEFORE UPDATE ON project_models
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_diagrams_updated_at
    BEFORE UPDATE ON diagrams
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
