-- Store sanitized provider event metadata for reconciliation/operational follow-up.
-- Raw Razorpay payloads and customer/card data are intentionally not persisted.
ALTER TABLE payment_webhook_events
    ADD COLUMN provider_object_id varchar(120),
    ADD COLUMN provider_order_id varchar(120),
    ADD COLUMN provider_payment_id varchar(120),
    ADD COLUMN amount_paise bigint;
CREATE INDEX idx_payment_webhook_provider_object ON payment_webhook_events(provider_object_id);
CREATE INDEX idx_payment_webhook_provider_order ON payment_webhook_events(provider_order_id);
