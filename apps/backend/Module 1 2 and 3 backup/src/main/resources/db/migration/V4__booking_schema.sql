CREATE TABLE booking_slots (
    id UUID PRIMARY KEY,
    facility_id UUID NOT NULL REFERENCES facilities(id),
    slot_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    capacity INTEGER NOT NULL,
    held_count INTEGER NOT NULL DEFAULT 0,
    booked_count INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_booking_slot UNIQUE (facility_id, slot_date, start_time),
    CONSTRAINT chk_booking_slot_time CHECK (end_time > start_time),
    CONSTRAINT chk_booking_slot_counts CHECK (capacity > 0 AND held_count >= 0 AND booked_count >= 0 AND held_count + booked_count <= capacity)
);

CREATE TABLE booking_holds (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    slot_id UUID NOT NULL REFERENCES booking_slots(id),
    quantity INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_booking_hold_quantity CHECK (quantity > 0)
);

CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    booking_number VARCHAR(30) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id),
    slot_id UUID NOT NULL REFERENCES booking_slots(id),
    quantity INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    confirmed_at TIMESTAMPTZ NOT NULL,
    cancelled_at TIMESTAMPTZ,
    CONSTRAINT chk_booking_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_booking_slots_facility_date ON booking_slots(facility_id, slot_date);
CREATE INDEX idx_booking_holds_expiry ON booking_holds(status, expires_at);
CREATE INDEX idx_booking_holds_user ON booking_holds(user_id, created_at);
CREATE INDEX idx_bookings_user ON bookings(user_id, confirmed_at);
CREATE INDEX idx_bookings_slot ON bookings(slot_id);
