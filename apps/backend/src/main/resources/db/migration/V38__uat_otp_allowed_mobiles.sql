CREATE TABLE uat_otp_allowed_mobiles (
 mobile VARCHAR(10) PRIMARY KEY CHECK (mobile ~ '^[0-9]{10}$'),
 active BOOLEAN NOT NULL DEFAULT TRUE,
 note VARCHAR(200),
 created_by UUID REFERENCES users(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
