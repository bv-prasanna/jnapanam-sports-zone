-- Versioned billing snapshot for payments, including discounted purchases.
-- The advertised amount remains the gross consideration; the configured tax rate is extracted from that amount.
CREATE TABLE venue_billing_purpose_policies (
 purpose varchar(20) PRIMARY KEY CHECK(purpose IN ('MEMBERSHIP','TRAINING','TOURNAMENT')),
 tax_rate_percent numeric(7,3) NOT NULL CHECK(tax_rate_percent>=0 AND tax_rate_percent<=100),
 active boolean NOT NULL DEFAULT false,
 updated_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE payment_orders
 ADD COLUMN billing_gross_paise bigint,
 ADD COLUMN billing_taxable_paise bigint,
 ADD COLUMN billing_tax_paise bigint,
 ADD COLUMN billing_rate_percent numeric(7,3),
 ADD COLUMN billing_loyalty_tender_paise bigint,
 ADD COLUMN billing_promotion_treatment varchar(30),
 ADD COLUMN billing_loyalty_treatment varchar(30),
 ADD COLUMN billing_snapshot_at timestamptz,
 ADD CONSTRAINT chk_billing_snapshot_nonnegative CHECK (
  (billing_gross_paise IS NULL OR billing_gross_paise>=0) AND
  (billing_taxable_paise IS NULL OR billing_taxable_paise>=0) AND
  (billing_tax_paise IS NULL OR billing_tax_paise>=0) AND
  (billing_loyalty_tender_paise IS NULL OR billing_loyalty_tender_paise>=0)
 );
ALTER TABLE venue_billing_profile
 ADD COLUMN coupon_tax_treatment varchar(30) NOT NULL DEFAULT 'REVIEW_REQUIRED'
 CHECK(coupon_tax_treatment IN ('REVIEW_REQUIRED','INVOICE_DISCOUNT')),
 ADD COLUMN loyalty_tax_treatment varchar(30) NOT NULL DEFAULT 'REVIEW_REQUIRED'
 CHECK(loyalty_tax_treatment IN ('REVIEW_REQUIRED','PAYMENT_TENDER','INVOICE_DISCOUNT')),
 ADD COLUMN e_invoice_applicable boolean,
 ADD COLUMN e_invoice_provider varchar(40),
 ADD COLUMN invoice_email_enabled boolean NOT NULL DEFAULT false;
