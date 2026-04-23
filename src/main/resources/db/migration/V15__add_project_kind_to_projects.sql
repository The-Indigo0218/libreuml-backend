CREATE TYPE project_kind AS ENUM ('SOFTWARE_ARCHITECTURE', 'FREE');

ALTER TABLE projects
    ADD COLUMN project_kind project_kind NULL DEFAULT 'FREE';

UPDATE projects SET project_kind = 'FREE' WHERE project_kind IS NULL;
