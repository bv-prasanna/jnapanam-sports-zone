CREATE TABLE notification_provider_settings(
 setting_key VARCHAR(80) PRIMARY KEY,
 encrypted_value TEXT NOT NULL,
 updated_by UUID REFERENCES users(id),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
