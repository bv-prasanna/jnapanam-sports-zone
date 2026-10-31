ALTER TABLE booking_address_proofs
  ADD COLUMN reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
  ADD COLUMN review_note VARCHAR(1000);

CREATE INDEX idx_booking_address_proofs_status_uploaded
  ON booking_address_proofs(status, uploaded_at DESC);
