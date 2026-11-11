-- Transactional, retryable invoice PDF delivery. No customer data is embedded in job logs.
CREATE TABLE venue_billing_email_deliveries (
 id uuid PRIMARY KEY,
 document_id uuid NOT NULL UNIQUE REFERENCES venue_billing_documents(id),
 payment_order_id uuid NOT NULL REFERENCES payment_orders(id),
 recipient varchar(254) NOT NULL,
 status varchar(16) NOT NULL DEFAULT 'QUEUED' CHECK (status IN ('QUEUED','SENT','FAILED')),
 attempts integer NOT NULL DEFAULT 0 CHECK (attempts>=0),
 last_error varchar(250),
 queued_at timestamptz NOT NULL DEFAULT now(),
 attempted_at timestamptz,
 sent_at timestamptz
);
CREATE INDEX idx_venue_billing_delivery_status ON venue_billing_email_deliveries(status,attempts,queued_at);
