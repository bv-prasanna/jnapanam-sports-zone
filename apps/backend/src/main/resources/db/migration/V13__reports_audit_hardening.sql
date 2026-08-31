ALTER TABLE audit_logs ADD COLUMN action VARCHAR(80);
ALTER TABLE audit_logs ADD COLUMN resource_type VARCHAR(80);
ALTER TABLE audit_logs ADD COLUMN resource_id VARCHAR(100);
ALTER TABLE audit_logs ADD COLUMN query_string VARCHAR(1000);
ALTER TABLE audit_logs ADD COLUMN correlation_id VARCHAR(80);
ALTER TABLE audit_logs ADD COLUMN user_agent VARCHAR(300);
ALTER TABLE audit_logs ADD COLUMN duration_ms BIGINT;
ALTER TABLE audit_logs ADD COLUMN successful BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE audit_logs
SET action = http_method || ' ' || request_path,
    resource_type = 'ADMIN_API',
    successful = response_status BETWEEN 200 AND 399
WHERE action IS NULL;

ALTER TABLE audit_logs ALTER COLUMN action SET NOT NULL;
ALTER TABLE audit_logs ALTER COLUMN resource_type SET NOT NULL;

CREATE INDEX idx_audit_resource ON audit_logs(resource_type, occurred_at DESC);
CREATE INDEX idx_audit_action ON audit_logs(action, occurred_at DESC);
CREATE INDEX idx_audit_correlation ON audit_logs(correlation_id);
CREATE INDEX idx_audit_status ON audit_logs(successful, occurred_at DESC);

CREATE OR REPLACE FUNCTION prevent_audit_log_change() RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'Audit logs are immutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_logs_no_update
BEFORE UPDATE OR DELETE ON audit_logs
FOR EACH ROW EXECUTE FUNCTION prevent_audit_log_change();
