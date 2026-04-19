CREATE TABLE audit_logs (
    id          UUID                     PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID                     REFERENCES users(id) ON DELETE SET NULL,
    event_type  VARCHAR(64)              NOT NULL,
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(512),
    metadata    TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_user_id    ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_event_type ON audit_logs(event_type);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
