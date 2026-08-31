CREATE TABLE training_programs (
 id UUID PRIMARY KEY, code VARCHAR(40) NOT NULL UNIQUE, sport_id UUID NOT NULL REFERENCES sports(id),
 name VARCHAR(140) NOT NULL, description VARCHAR(700), min_age INTEGER, max_age INTEGER,
 level VARCHAR(30) NOT NULL, status VARCHAR(20) NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT chk_training_ages CHECK((min_age IS NULL OR min_age >= 3) AND (max_age IS NULL OR max_age >= min_age))
);
CREATE TABLE training_batches (
 id UUID PRIMARY KEY, program_id UUID NOT NULL REFERENCES training_programs(id), facility_id UUID NOT NULL REFERENCES facilities(id),
 coach_user_id UUID REFERENCES users(id), name VARCHAR(140) NOT NULL, start_date DATE NOT NULL, end_date DATE NOT NULL,
 start_time TIME NOT NULL, end_time TIME NOT NULL, days_of_week VARCHAR(80) NOT NULL, capacity INTEGER NOT NULL,
 fee NUMERIC(12,2) NOT NULL, status VARCHAR(20) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT chk_training_batch_dates CHECK(end_date >= start_date), CONSTRAINT chk_training_batch_time CHECK(end_time > start_time),
 CONSTRAINT chk_training_batch_capacity CHECK(capacity > 0), CONSTRAINT chk_training_batch_fee CHECK(fee >= 0)
);
CREATE TABLE training_enrollments (
 id UUID PRIMARY KEY, batch_id UUID NOT NULL REFERENCES training_batches(id), user_id UUID NOT NULL REFERENCES users(id),
 participant_name VARCHAR(120) NOT NULL, participant_age INTEGER NOT NULL, status VARCHAR(20) NOT NULL,
 enrolled_at TIMESTAMPTZ NOT NULL, cancelled_at TIMESTAMPTZ, CONSTRAINT uq_training_enrollment UNIQUE(batch_id,user_id),
 CONSTRAINT chk_training_participant_age CHECK(participant_age BETWEEN 3 AND 100)
);
CREATE TABLE training_attendance (
 id UUID PRIMARY KEY, enrollment_id UUID NOT NULL REFERENCES training_enrollments(id) ON DELETE CASCADE,
 session_date DATE NOT NULL, present BOOLEAN NOT NULL, marked_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT uq_training_attendance UNIQUE(enrollment_id,session_date)
);
CREATE INDEX idx_training_batches_program ON training_batches(program_id,start_date);
CREATE INDEX idx_training_enrollments_batch ON training_enrollments(batch_id,status);
