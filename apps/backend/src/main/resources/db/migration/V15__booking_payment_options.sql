ALTER TABLE users
    ADD COLUMN requires_upfront_payment BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE bookings
    ADD COLUMN payment_option VARCHAR(30),
    ADD COLUMN payment_status VARCHAR(30) NOT NULL DEFAULT 'UNPAID',
    ADD COLUMN onsite_payment_method VARCHAR(30),
    ADD COLUMN onsite_payment_reference VARCHAR(120),
    ADD COLUMN onsite_paid_at TIMESTAMPTZ,
    ADD COLUMN onsite_collected_by UUID REFERENCES users(id);

UPDATE bookings b
SET payment_option = CASE WHEN f.booking_price > 0 THEN 'PAY_NOW' ELSE NULL END,
    payment_status = CASE
        WHEN f.booking_price <= 0 THEN 'NOT_REQUIRED'
        WHEN b.payment_completed_at IS NOT NULL THEN 'PAID'
        ELSE 'UNPAID'
    END
FROM booking_slots s
JOIN facilities f ON f.id = s.facility_id
WHERE s.id = b.slot_id;

CREATE INDEX idx_bookings_payment_status ON bookings(payment_status);
CREATE INDEX idx_users_upfront_payment ON users(requires_upfront_payment) WHERE requires_upfront_payment = TRUE;
