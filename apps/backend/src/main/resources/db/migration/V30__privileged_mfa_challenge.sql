ALTER TABLE privileged_mfa ADD COLUMN IF NOT EXISTS challenge_hash VARCHAR(255); ALTER TABLE privileged_mfa ADD COLUMN IF NOT EXISTS challenge_expires_at TIMESTAMPTZ;
