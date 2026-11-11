-- Every mutating API event carries auditable location metadata, even when GPS unavailable.
ALTER TABLE audit_logs ADD COLUMN latitude NUMERIC(10,7);
ALTER TABLE audit_logs ADD COLUMN longitude NUMERIC(10,7);
ALTER TABLE audit_logs ADD COLUMN location_status VARCHAR(25) NOT NULL DEFAULT 'UNAVAILABLE';
ALTER TABLE audit_logs ADD COLUMN location_source VARCHAR(25);
ALTER TABLE audit_logs ADD CONSTRAINT audit_lat_bounds CHECK(latitude BETWEEN -90 AND 90);
ALTER TABLE audit_logs ADD CONSTRAINT audit_lon_bounds CHECK(longitude BETWEEN -180 AND 180);
ALTER TABLE audit_logs ADD CONSTRAINT audit_location_pair CHECK ((latitude IS NULL) = (longitude IS NULL));
ALTER TABLE audit_logs ADD CONSTRAINT audit_location_status CHECK(location_status IN ('CAPTURED','DENIED','UNAVAILABLE','INVALID','NOT_REQUESTED'));
CREATE INDEX idx_audit_geo ON audit_logs(occurred_at DESC) WHERE location_status='CAPTURED';
