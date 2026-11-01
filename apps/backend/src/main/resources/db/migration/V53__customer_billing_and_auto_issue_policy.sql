-- Auto-issue is disabled until the seller approves tax timing and place of supply for each service type.
ALTER TABLE venue_billing_purpose_policies DROP CONSTRAINT IF EXISTS venue_billing_purpose_policies_purpose_check;
ALTER TABLE venue_billing_purpose_policies ADD CONSTRAINT chk_purpose_tax_policy
 CHECK (purpose IN ('BOOKING','MEMBERSHIP','TRAINING','TOURNAMENT'));
ALTER TABLE venue_billing_purpose_policies
 ADD COLUMN place_of_supply_state_code varchar(2),
 ADD COLUMN auto_issue_enabled boolean NOT NULL DEFAULT false,
 ADD CONSTRAINT chk_purpose_place_code CHECK(place_of_supply_state_code IS NULL OR place_of_supply_state_code ~ '^[0-9]{2}$');
CREATE TABLE customer_billing_profiles(
 user_id uuid PRIMARY KEY REFERENCES users(id),
 recipient_address varchar(400) NOT NULL,
 state_code varchar(2) NOT NULL CHECK(state_code ~ '^[0-9]{2}$'),
 gstin varchar(15) CHECK(gstin IS NULL OR gstin ~ '^[0-9]{2}[A-Z0-9]{13}$'),
 updated_at timestamptz NOT NULL DEFAULT now()
);
