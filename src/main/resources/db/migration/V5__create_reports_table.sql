CREATE TABLE reports
(
    id               UUID PRIMARY KEY,

    user_id          UUID         NOT NULL,

    type             VARCHAR(50)  NOT NULL,
    status           VARCHAR(50)  NOT NULL DEFAULT 'OPEN',
    priority         VARCHAR(50),
    title            VARCHAR(255) NOT NULL,

    description      TEXT,
    admin_response   TEXT,
    internal_notes   TEXT,

    created_at       TIMESTAMP    NOT NULL,
    solved_at        TIMESTAMP,

    evidences_images JSONB,

    CONSTRAINT fk_reports_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_reports_user_id ON reports (user_id);