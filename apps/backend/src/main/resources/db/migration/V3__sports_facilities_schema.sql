CREATE TABLE sports (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    icon_key VARCHAR(60),
    display_order INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    suspended_from DATE,
    suspended_until DATE,
    status_reason VARCHAR(250),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE facilities (
    id UUID PRIMARY KEY,
    sport_id UUID NOT NULL REFERENCES sports(id),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    location_label VARCHAR(160),
    slot_duration_minutes INTEGER NOT NULL,
    capacity INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    suspended_from DATE,
    suspended_until DATE,
    status_reason VARCHAR(250),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_facility_slot_duration CHECK (slot_duration_minutes BETWEEN 15 AND 240),
    CONSTRAINT chk_facility_capacity CHECK (capacity > 0)
);

CREATE TABLE facility_operating_hours (
    id UUID PRIMARY KEY,
    facility_id UUID NOT NULL REFERENCES facilities(id) ON DELETE CASCADE,
    day_of_week VARCHAR(10) NOT NULL,
    open_time TIME,
    close_time TIME,
    closed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_facility_day UNIQUE (facility_id, day_of_week),
    CONSTRAINT chk_operating_hours CHECK (
        (closed = TRUE AND open_time IS NULL AND close_time IS NULL)
        OR (closed = FALSE AND open_time IS NOT NULL AND close_time IS NOT NULL AND close_time > open_time)
    )
);

CREATE TABLE facility_closures (
    id UUID PRIMARY KEY,
    facility_id UUID NOT NULL REFERENCES facilities(id) ON DELETE CASCADE,
    closure_date DATE NOT NULL,
    reason VARCHAR(250) NOT NULL,
    CONSTRAINT uq_facility_closure_date UNIQUE (facility_id, closure_date)
);

CREATE TABLE facility_schedule_overrides (
    id UUID PRIMARY KEY,
    facility_id UUID NOT NULL REFERENCES facilities(id) ON DELETE CASCADE,
    schedule_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    availability VARCHAR(20) NOT NULL,
    slot_duration_minutes INTEGER,
    capacity INTEGER,
    reason VARCHAR(250),
    CONSTRAINT chk_schedule_override_time CHECK (end_time > start_time),
    CONSTRAINT chk_schedule_override_duration CHECK (slot_duration_minutes IS NULL OR slot_duration_minutes BETWEEN 15 AND 240),
    CONSTRAINT chk_schedule_override_capacity CHECK (capacity IS NULL OR capacity > 0)
);

CREATE INDEX idx_facilities_sport ON facilities(sport_id);
CREATE INDEX idx_sports_status ON sports(status);
CREATE INDEX idx_facilities_status ON facilities(status);
CREATE INDEX idx_closures_facility_date ON facility_closures(facility_id, closure_date);
CREATE INDEX idx_schedule_overrides_facility_date ON facility_schedule_overrides(facility_id, schedule_date);

INSERT INTO sports(id, code, name, description, icon_key, display_order, status, created_at, updated_at) VALUES
('10000000-0000-0000-0000-000000000001', 'SWIMMING', 'Swimming', 'Structured pool sessions, coaching and recreation for every age.', 'swim', 1, 'ACTIVE', NOW(), NOW()),
('10000000-0000-0000-0000-000000000002', 'CRICKET', 'Cricket', 'Practice nets, skill coaching and match-ready development.', 'cricket', 2, 'ACTIVE', NOW(), NOW()),
('10000000-0000-0000-0000-000000000003', 'BADMINTON', 'Badminton', 'Quality courts and coaching for beginners and competitors.', 'badminton', 3, 'ACTIVE', NOW(), NOW()),
('10000000-0000-0000-0000-000000000004', 'CARROM', 'Carrom', 'Focus, control and friendly indoor competition.', 'carrom', 4, 'ACTIVE', NOW(), NOW()),
('10000000-0000-0000-0000-000000000005', 'CHESS', 'Chess', 'Build strategy, patience and confident decision-making.', 'chess', 5, 'ACTIVE', NOW(), NOW()),
('10000000-0000-0000-0000-000000000006', 'GYM', 'Gym', 'Guided strength and conditioning in a supportive environment.', 'dumbbell', 6, 'ACTIVE', NOW(), NOW()),
('10000000-0000-0000-0000-000000000007', 'AEROBICS', 'Aerobics', 'Energetic group sessions for mobility, stamina and well-being.', 'aerobics', 7, 'ACTIVE', NOW(), NOW());
