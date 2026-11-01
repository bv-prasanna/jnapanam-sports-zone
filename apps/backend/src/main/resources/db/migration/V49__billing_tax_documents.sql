-- Seller tax identity is intentionally unconfigured until the operator supplies legal details.
CREATE TABLE venue_billing_profile (
 id smallint PRIMARY KEY DEFAULT 1 CHECK(id=1),
 legal_name varchar(160) NOT NULL,
 gstin varchar(15),
 registered_address varchar(400) NOT NULL,
 state_code varchar(2),
 service_accounting_code varchar(12),
 signatory varchar(120),
 updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE SEQUENCE venue_billing_document_seq START WITH 1;
CREATE TABLE venue_billing_documents (
 id uuid PRIMARY KEY,
 payment_order_id uuid NOT NULL REFERENCES payment_orders(id),
 refund_id uuid REFERENCES payment_refunds(id),
 kind varchar(16) NOT NULL CHECK(kind IN ('INVOICE','CREDIT_NOTE')),
 state varchar(16) NOT NULL CHECK(state IN ('DRAFT','ISSUED')),
 document_number varchar(16) UNIQUE,
 fiscal_year integer NOT NULL,
 taxable_paise bigint NOT NULL CHECK(taxable_paise >= 0),
 tax_paise bigint NOT NULL CHECK(tax_paise >= 0),
 total_paise bigint NOT NULL CHECK(total_paise >= 0),
 document_data jsonb NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now(),
 issued_at timestamptz,
 CHECK ((state='ISSUED' AND document_number IS NOT NULL AND issued_at IS NOT NULL) OR
        (state='DRAFT' AND document_number IS NULL AND issued_at IS NULL))
);
CREATE UNIQUE INDEX uq_booking_billing_invoice ON venue_billing_documents(payment_order_id) WHERE kind='INVOICE';
CREATE UNIQUE INDEX uq_booking_billing_credit_refund ON venue_billing_documents(refund_id) WHERE kind='CREDIT_NOTE';
