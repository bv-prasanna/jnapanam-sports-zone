-- Separate recurring billing configuration and mandate audit from one-time payment orders.
CREATE TABLE recurring_offerings (
 id uuid primary key,
 kind varchar(15) not null check(kind in('MEMBERSHIP','TRAINING')),
 reference_id uuid not null,
 title varchar(150) not null,
 amount_paise bigint not null check(amount_paise>=100),
 period varchar(12) not null check(period in ('weekly','monthly','yearly')),
 interval_count integer not null check(interval_count between 1 and 12),
 total_cycles integer not null check(total_cycles between 2 and 60),
 razorpay_plan_id varchar(120) not null unique,
 enabled boolean not null default false,
 created_at timestamptz not null default now(),
 unique(kind,reference_id)
);
CREATE TABLE recurring_agreements (
 id uuid primary key,
 offering_id uuid not null references recurring_offerings(id),
 owner_id uuid not null references users(id),
 reference_id uuid not null,
 kind varchar(15) not null,
 provider_subscription_id varchar(120) not null unique,
 authorization_token_hash varchar(64) not null unique,
 authorization_expires_at timestamptz not null,
 status varchar(25) not null default 'CREATED',
 paid_cycles integer not null default 0,
 last_charge_at timestamptz,
 created_at timestamptz not null default now(),
 updated_at timestamptz not null default now(),
 unique(kind,reference_id),
 constraint chk_recurring_agreement_status check(status in
 ('CREATED','AUTHENTICATED','ACTIVE','PENDING','HALTED','PAUSED','CANCELLED','COMPLETED','EXPIRED'))
);
CREATE INDEX idx_recurring_agreements_owner on recurring_agreements(owner_id,created_at desc);
CREATE TABLE recurring_charges (
 id uuid primary key,
 agreement_id uuid not null references recurring_agreements(id),
 provider_payment_id varchar(120) not null unique,
 amount_paise bigint not null,
 cycle_status varchar(25) not null default 'CHARGED',
 charged_at timestamptz not null default now()
);
CREATE INDEX idx_recurring_charges_agreement on recurring_charges(agreement_id,charged_at desc);

CREATE TABLE recurring_charge_refunds (
 id uuid primary key,
 charge_id uuid not null references recurring_charges(id),
 provider_refund_id varchar(120) not null unique,
 amount_paise bigint not null check (amount_paise>0),
 status varchar(20) not null check (status in ('CREATED','PROCESSED','FAILED')),
 updated_at timestamptz not null default now()
);
CREATE INDEX idx_recurring_refunds_charge on recurring_charge_refunds(charge_id);
