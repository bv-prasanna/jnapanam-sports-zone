CREATE TABLE membership_plans (
 id UUID PRIMARY KEY, code VARCHAR(40) NOT NULL UNIQUE, name VARCHAR(120) NOT NULL,
 description VARCHAR(600), duration_days INTEGER NOT NULL, price NUMERIC(12,2) NOT NULL,
 daily_booking_limit INTEGER, monthly_booking_limit INTEGER, status VARCHAR(20) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT chk_membership_duration CHECK(duration_days > 0),
 CONSTRAINT chk_membership_price CHECK(price >= 0),
 CONSTRAINT chk_membership_limits CHECK((daily_booking_limit IS NULL OR daily_booking_limit > 0) AND (monthly_booking_limit IS NULL OR monthly_booking_limit > 0))
);
CREATE TABLE membership_plan_sports (
 plan_id UUID NOT NULL REFERENCES membership_plans(id) ON DELETE CASCADE,
 sport_id UUID NOT NULL REFERENCES sports(id), PRIMARY KEY(plan_id,sport_id)
);
CREATE TABLE customer_memberships (
 id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES users(id), plan_id UUID NOT NULL REFERENCES membership_plans(id),
 status VARCHAR(30) NOT NULL, start_date DATE, end_date DATE, subscribed_at TIMESTAMPTZ NOT NULL,
 activated_at TIMESTAMPTZ, updated_at TIMESTAMPTZ NOT NULL, status_reason VARCHAR(250),
 CONSTRAINT chk_membership_dates CHECK(start_date IS NULL OR end_date IS NULL OR end_date >= start_date)
);
CREATE INDEX idx_customer_membership_user ON customer_memberships(user_id,subscribed_at);
CREATE INDEX idx_customer_membership_status ON customer_memberships(status);
