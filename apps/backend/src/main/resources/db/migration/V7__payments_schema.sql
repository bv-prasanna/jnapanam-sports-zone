ALTER TABLE facilities ADD COLUMN booking_price NUMERIC(12,2) NOT NULL DEFAULT 0;

CREATE TABLE payment_orders (
 id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES users(id), purpose VARCHAR(20) NOT NULL,
 reference_id UUID NOT NULL, receipt_number VARCHAR(40) NOT NULL UNIQUE,
 amount_paise BIGINT NOT NULL CHECK(amount_paise >= 0), currency VARCHAR(3) NOT NULL,
 provider VARCHAR(20) NOT NULL, provider_order_id VARCHAR(120) UNIQUE,
 provider_payment_id VARCHAR(120) UNIQUE, status VARCHAR(30) NOT NULL,
 idempotency_key VARCHAR(100) NOT NULL, paid_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uq_payment_idempotency UNIQUE(user_id,idempotency_key)
);
CREATE INDEX idx_payment_user_created ON payment_orders(user_id,created_at DESC);
CREATE INDEX idx_payment_reference ON payment_orders(purpose,reference_id);

CREATE TABLE payment_refunds (
 id UUID PRIMARY KEY, payment_order_id UUID NOT NULL REFERENCES payment_orders(id),
 provider_refund_id VARCHAR(120) UNIQUE, amount_paise BIGINT NOT NULL CHECK(amount_paise > 0),
 reason VARCHAR(300) NOT NULL, status VARCHAR(20) NOT NULL,
 requested_at TIMESTAMPTZ NOT NULL, processed_at TIMESTAMPTZ
);

CREATE TABLE payment_webhook_events (
 id UUID PRIMARY KEY, provider_event_id VARCHAR(160) NOT NULL UNIQUE,
 event_type VARCHAR(80) NOT NULL, payload_hash VARCHAR(64) NOT NULL,
 status VARCHAR(20) NOT NULL, processed_at TIMESTAMPTZ NOT NULL
);
