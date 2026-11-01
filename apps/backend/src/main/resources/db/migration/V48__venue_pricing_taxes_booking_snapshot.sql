-- Scoped, versioned arena pricing and tax rules. Tax rules are applied only after explicit configuration.
CREATE TABLE venue_price_rules (
 id uuid PRIMARY KEY,
 name varchar(120) NOT NULL,
 scope varchar(16) NOT NULL CHECK (scope IN ('SITE','SPORT','FACILITY','SLOT')),
 target varchar(80),
 adjustment varchar(24) NOT NULL CHECK (adjustment IN ('FIXED_PRICE','ADD_AMOUNT','DISCOUNT_AMOUNT','ADD_PERCENT','DISCOUNT_PERCENT')),
 value numeric(12,2) NOT NULL CHECK (value >= 0),
 priority integer NOT NULL DEFAULT 0,
 effective_from date,
 effective_to date,
 start_time time,
 end_time time,
 weekdays varchar(80),
 active boolean NOT NULL DEFAULT true,
 created_at timestamptz NOT NULL DEFAULT now(),
 updated_at timestamptz NOT NULL DEFAULT now(),
 CHECK ((scope = 'SITE' AND target IS NULL) OR (scope <> 'SITE' AND target IS NOT NULL)),
 CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to >= effective_from),
 CHECK (end_time IS NULL OR start_time IS NULL OR end_time > start_time)
);
CREATE INDEX idx_venue_price_rules_scope ON venue_price_rules(active,scope,target);
CREATE TABLE venue_tax_rules (
 id uuid PRIMARY KEY,
 name varchar(120) NOT NULL,
 scope varchar(16) NOT NULL CHECK (scope IN ('SITE','SPORT','FACILITY','SLOT')),
 target varchar(80),
 rate_percent numeric(7,3) NOT NULL CHECK (rate_percent >= 0 AND rate_percent <= 100),
 inclusive boolean NOT NULL DEFAULT false,
 effective_from date,
 effective_to date,
 active boolean NOT NULL DEFAULT true,
 created_at timestamptz NOT NULL DEFAULT now(),
 updated_at timestamptz NOT NULL DEFAULT now(),
 CHECK ((scope = 'SITE' AND target IS NULL) OR (scope <> 'SITE' AND target IS NOT NULL)),
 CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to >= effective_from)
);
CREATE INDEX idx_venue_tax_rules_scope ON venue_tax_rules(active,scope,target);
-- Capture agreed amounts before payment. Nulls preserve legacy bookings that precede this feature.
ALTER TABLE bookings ADD COLUMN price_base_paise bigint,
 ADD COLUMN price_subtotal_paise bigint,
 ADD COLUMN price_tax_paise bigint,
 ADD COLUMN price_total_paise bigint,
 ADD COLUMN price_rule_id varchar(40),
 ADD COLUMN tax_rule_id varchar(40),
 ADD COLUMN price_locked_at timestamptz,
 ADD CONSTRAINT chk_booking_price_nonnegative CHECK (
 (price_base_paise IS NULL OR price_base_paise >= 0) AND
 (price_subtotal_paise IS NULL OR price_subtotal_paise >= 0) AND
 (price_tax_paise IS NULL OR price_tax_paise >= 0) AND
 (price_total_paise IS NULL OR price_total_paise >= 0)
 );
