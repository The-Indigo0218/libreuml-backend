ALTER TABLE projects
    ADD COLUMN project_kind VARCHAR(64) NULL DEFAULT 'FREE'
    CONSTRAINT chk_project_kind CHECK (project_kind IN ('SOFTWARE_ARCHITECTURE', 'FREE'));

UPDATE projects SET project_kind = 'FREE' WHERE project_kind IS NULL;
