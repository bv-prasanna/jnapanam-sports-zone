ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS device_signature_hash VARCHAR(64);
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS device_name VARCHAR(160);
CREATE TABLE mobile_otp_challenges(id UUID PRIMARY KEY,purpose VARCHAR(30) NOT NULL,mobile VARCHAR(20) NOT NULL,otp_hash VARCHAR(255) NOT NULL,payload_json TEXT,expires_at TIMESTAMPTZ NOT NULL,verified_at TIMESTAMPTZ,attempts INT NOT NULL DEFAULT 0,created_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE INDEX IF NOT EXISTS idx_mobile_otp_mobile_purpose ON mobile_otp_challenges(mobile,purpose,created_at DESC);
