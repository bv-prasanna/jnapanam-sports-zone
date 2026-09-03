ALTER TABLE bookings
    ADD COLUMN checked_out_at TIMESTAMPTZ,
    ADD COLUMN checked_out_by UUID REFERENCES users(id),
    ADD COLUMN identity_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN verification_note VARCHAR(120);

CREATE INDEX idx_bookings_checked_out_at ON bookings(checked_out_at);
