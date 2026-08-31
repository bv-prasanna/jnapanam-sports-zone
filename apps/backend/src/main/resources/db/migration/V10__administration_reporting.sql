ALTER TABLE bookings ADD COLUMN checked_in_at TIMESTAMPTZ;
ALTER TABLE bookings ADD COLUMN checked_in_by UUID REFERENCES users(id);

CREATE TABLE audit_logs(
 id UUID PRIMARY KEY,actor_user_id UUID REFERENCES users(id),http_method VARCHAR(10) NOT NULL,
 request_path VARCHAR(300) NOT NULL,response_status INTEGER NOT NULL,remote_address VARCHAR(80),
 occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_audit_occurred ON audit_logs(occurred_at DESC);
CREATE INDEX idx_audit_actor ON audit_logs(actor_user_id,occurred_at DESC);
