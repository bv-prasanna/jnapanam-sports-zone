CREATE TABLE booking_address_proofs (
 id UUID PRIMARY KEY,
 user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 document_type VARCHAR(20) NOT NULL CHECK (document_type IN ('DL','VOTER_ID','PASSPORT')),
 content_type VARCHAR(40) NOT NULL CHECK (content_type IN ('image/jpeg','image/png','application/pdf')),
 original_filename VARCHAR(180),
 document_bytes BYTEA NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED')),
 uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 reviewed_at TIMESTAMPTZ
);
CREATE INDEX idx_booking_address_proofs_user ON booking_address_proofs(user_id,uploaded_at DESC);
