-- Multiple non-overlapping windows are validated atomically by FacilityScheduleService.
ALTER TABLE facility_operating_hours DROP CONSTRAINT IF EXISTS uq_facility_day;
CREATE INDEX IF NOT EXISTS idx_facility_hours_day_start
    ON facility_operating_hours(facility_id, day_of_week, open_time);

ALTER TABLE facilities ADD COLUMN IF NOT EXISTS buffer_minutes INTEGER NOT NULL DEFAULT 0;
ALTER TABLE facilities ADD COLUMN IF NOT EXISTS terms_and_conditions VARCHAR(3000);
ALTER TABLE facilities ADD CONSTRAINT chk_facility_buffer_minutes CHECK (buffer_minutes BETWEEN 0 AND 120);

ALTER TABLE bookings ADD COLUMN IF NOT EXISTS terms_accepted_at TIMESTAMPTZ;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS terms_snapshot VARCHAR(3000);
