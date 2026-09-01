CREATE TABLE promotions (
 id UUID PRIMARY KEY, code VARCHAR(40) NOT NULL UNIQUE, name VARCHAR(120) NOT NULL,
 description VARCHAR(500), discount_type VARCHAR(20) NOT NULL, discount_value NUMERIC(12,2) NOT NULL,
 max_discount_paise BIGINT, min_order_paise BIGINT NOT NULL DEFAULT 0,
 purpose VARCHAR(20), automatic BOOLEAN NOT NULL DEFAULT FALSE, online_only BOOLEAN NOT NULL DEFAULT TRUE,
 starts_at TIMESTAMPTZ NOT NULL, ends_at TIMESTAMPTZ NOT NULL,
 global_usage_limit INTEGER, per_user_limit INTEGER NOT NULL DEFAULT 1,
 usage_count INTEGER NOT NULL DEFAULT 0, active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT chk_promotion_dates CHECK (ends_at > starts_at),
 CONSTRAINT chk_promotion_value CHECK (discount_value > 0),
 CONSTRAINT chk_promotion_limits CHECK (min_order_paise >= 0 AND (global_usage_limit IS NULL OR global_usage_limit > 0) AND per_user_limit > 0)
);
CREATE INDEX idx_promotions_active_dates ON promotions(active, starts_at, ends_at);

CREATE TABLE promotion_redemptions (
 id UUID PRIMARY KEY, promotion_id UUID NOT NULL REFERENCES promotions(id),
 user_id UUID NOT NULL REFERENCES users(id), payment_order_id UUID NOT NULL UNIQUE REFERENCES payment_orders(id),
 discount_paise BIGINT NOT NULL, redeemed_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_promotion_redemption_user ON promotion_redemptions(promotion_id,user_id);

CREATE TABLE loyalty_wallets (
 user_id UUID PRIMARY KEY REFERENCES users(id), points_balance BIGINT NOT NULL DEFAULT 0,
 lifetime_earned BIGINT NOT NULL DEFAULT 0, lifetime_redeemed BIGINT NOT NULL DEFAULT 0,
 updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT chk_loyalty_values CHECK (points_balance >= 0 AND lifetime_earned >= 0 AND lifetime_redeemed >= 0)
);

CREATE TABLE loyalty_transactions (
 id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES users(id), payment_order_id UUID REFERENCES payment_orders(id),
 transaction_type VARCHAR(20) NOT NULL, points BIGINT NOT NULL, description VARCHAR(250) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_loyalty_transactions_user ON loyalty_transactions(user_id,created_at DESC);

ALTER TABLE payment_orders
 ADD COLUMN original_amount_paise BIGINT,
 ADD COLUMN discount_paise BIGINT NOT NULL DEFAULT 0,
 ADD COLUMN promotion_code VARCHAR(40),
 ADD COLUMN loyalty_points_used BIGINT NOT NULL DEFAULT 0,
 ADD COLUMN loyalty_points_earned BIGINT NOT NULL DEFAULT 0;

UPDATE payment_orders SET original_amount_paise=amount_paise WHERE original_amount_paise IS NULL;
ALTER TABLE payment_orders ALTER COLUMN original_amount_paise SET NOT NULL;
