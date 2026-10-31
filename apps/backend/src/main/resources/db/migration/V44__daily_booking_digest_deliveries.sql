CREATE TABLE daily_booking_digest_deliveries (
 report_date date NOT NULL,
 recipient_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 status varchar(20) NOT NULL DEFAULT 'PENDING',
 attempts int NOT NULL DEFAULT 0,
 locked_at timestamptz,
 next_attempt_at timestamptz NOT NULL DEFAULT now(),
 sent_at timestamptz,
 provider_reference varchar(160),
 last_error varchar(500),
 created_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(report_date,recipient_user_id),
 CONSTRAINT chk_booking_digest_status CHECK (status IN ('PENDING','SENDING','SENT','FAILED','SKIPPED')),
 CONSTRAINT chk_booking_digest_attempts CHECK (attempts >= 0)
);
CREATE INDEX idx_daily_booking_digest_retry ON daily_booking_digest_deliveries(status,next_attempt_at);
