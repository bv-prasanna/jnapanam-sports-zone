ALTER TABLE bookings ADD COLUMN payment_expires_at TIMESTAMPTZ;
ALTER TABLE bookings ADD COLUMN payment_completed_at TIMESTAMPTZ;
ALTER TABLE bookings ADD COLUMN status_reason VARCHAR(300);

ALTER TABLE customer_memberships ADD COLUMN payment_expires_at TIMESTAMPTZ;

ALTER TABLE training_enrollments ADD COLUMN payment_expires_at TIMESTAMPTZ;
ALTER TABLE training_enrollments ADD COLUMN payment_completed_at TIMESTAMPTZ;

ALTER TABLE tournament_teams ADD COLUMN payment_expires_at TIMESTAMPTZ;
ALTER TABLE tournament_teams ADD COLUMN payment_completed_at TIMESTAMPTZ;

ALTER TABLE training_enrollments DROP CONSTRAINT uq_training_enrollment;
CREATE UNIQUE INDEX uq_training_active_enrollment ON training_enrollments(batch_id,user_id)
 WHERE status IN ('PENDING_PAYMENT','ENROLLED');
ALTER TABLE tournament_teams DROP CONSTRAINT uq_tournament_team_name;
ALTER TABLE tournament_teams DROP CONSTRAINT uq_tournament_captain;
CREATE UNIQUE INDEX uq_tournament_active_team_name ON tournament_teams(tournament_id,name)
 WHERE status IN ('PENDING_PAYMENT','REGISTERED');
CREATE UNIQUE INDEX uq_tournament_active_captain ON tournament_teams(tournament_id,captain_user_id)
 WHERE status IN ('PENDING_PAYMENT','REGISTERED');

CREATE TABLE password_reset_tokens(
 id UUID PRIMARY KEY,user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 token_hash VARCHAR(64) NOT NULL UNIQUE,expires_at TIMESTAMPTZ NOT NULL,
 used_at TIMESTAMPTZ,created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_password_reset_user ON password_reset_tokens(user_id,created_at DESC);
CREATE INDEX idx_password_reset_expiry ON password_reset_tokens(expires_at);

CREATE INDEX idx_booking_payment_expiry ON bookings(status,payment_expires_at);
CREATE INDEX idx_membership_payment_expiry ON customer_memberships(status,payment_expires_at);
CREATE INDEX idx_training_payment_expiry ON training_enrollments(status,payment_expires_at);
CREATE INDEX idx_team_payment_expiry ON tournament_teams(status,payment_expires_at);
