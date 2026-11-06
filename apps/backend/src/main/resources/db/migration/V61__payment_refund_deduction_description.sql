-- Store the administrator's explanation of charges retained from a booking refund.
-- Nullable for existing refunds and refunds with zero deduction.
ALTER TABLE payment_refunds
    ADD COLUMN deduction_description VARCHAR(300);
