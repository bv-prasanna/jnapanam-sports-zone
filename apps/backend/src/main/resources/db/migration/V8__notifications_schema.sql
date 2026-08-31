CREATE TABLE notification_templates(
 id UUID PRIMARY KEY, code VARCHAR(60) NOT NULL UNIQUE, channel VARCHAR(20) NOT NULL,
 subject VARCHAR(180), body TEXT NOT NULL, active BOOLEAN NOT NULL,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE notification_preferences(
 user_id UUID PRIMARY KEY REFERENCES users(id), email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
 sms_enabled BOOLEAN NOT NULL DEFAULT TRUE, push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
 updated_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE notifications(
 id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES users(id), channel VARCHAR(20) NOT NULL,
 category VARCHAR(40) NOT NULL, subject VARCHAR(180), message TEXT NOT NULL,
 status VARCHAR(20) NOT NULL, attempts INTEGER NOT NULL DEFAULT 0,
 provider_message_id VARCHAR(160), last_error VARCHAR(500), read_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL, sent_at TIMESTAMPTZ, next_attempt_at TIMESTAMPTZ
);
CREATE INDEX idx_notifications_user_created ON notifications(user_id,created_at DESC);
CREATE INDEX idx_notifications_outbox ON notifications(status,next_attempt_at);
INSERT INTO notification_templates(id,code,channel,subject,body,active,created_at,updated_at) VALUES
(gen_random_uuid(),'PAYMENT_SUCCESS','IN_APP','Payment successful','Payment {{receiptNumber}} of ₹{{amount}} was successful.',TRUE,now(),now()),
(gen_random_uuid(),'REFUND_PROCESSED','IN_APP','Refund processed','Refund of ₹{{amount}} for {{receiptNumber}} was processed.',TRUE,now(),now());
