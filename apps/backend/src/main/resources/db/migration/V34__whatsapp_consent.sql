ALTER TABLE notification_preferences ADD COLUMN IF NOT EXISTS whatsapp_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE notification_preferences ADD COLUMN IF NOT EXISTS whatsapp_consented_at TIMESTAMPTZ;
CREATE TABLE IF NOT EXISTS whatsapp_consent_history(id UUID PRIMARY KEY,user_id UUID NOT NULL REFERENCES users(id),enabled BOOLEAN NOT NULL,consented_at TIMESTAMPTZ NOT NULL DEFAULT now());
