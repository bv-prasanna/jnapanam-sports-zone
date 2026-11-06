-- Audit the retained portion of each administrator-approved refund without changing captured transaction totals.
ALTER TABLE payment_refunds
    ADD COLUMN deduction_paise BIGINT NOT NULL DEFAULT 0
    CONSTRAINT ck_payment_refund_deduction_nonnegative CHECK(deduction_paise >= 0);
CREATE INDEX idx_payment_refunds_order_requested
    ON payment_refunds(payment_order_id, requested_at DESC);
