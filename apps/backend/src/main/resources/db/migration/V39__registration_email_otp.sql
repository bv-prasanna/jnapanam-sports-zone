CREATE TABLE registration_email_otp_challenges (
 id UUID PRIMARY KEY,
 email VARCHAR(180) NOT NULL,
 otp_hash VARCHAR(255) NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL,
 verified_at TIMESTAMPTZ,
 attempts INTEGER NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_reg_email_otp_lookup ON registration_email_otp_challenges(email, created_at DESC);
