ALTER TABLE loyalty_wallets ADD COLUMN IF NOT EXISTS earned_points_reversal_debt bigint NOT NULL DEFAULT 0 CHECK(earned_points_reversal_debt>=0);
