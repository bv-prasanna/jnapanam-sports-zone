CREATE TABLE customer_dependents (
 id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES users(id), full_name VARCHAR(160) NOT NULL, date_of_birth DATE NOT NULL,
 relationship VARCHAR(60) NOT NULL, guardian_consent BOOLEAN NOT NULL DEFAULT FALSE, waiver_version VARCHAR(40), consented_at TIMESTAMPTZ,
 active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_customer_dependents_user ON customer_dependents(user_id);

CREATE TABLE support_tickets (
 id UUID PRIMARY KEY, ticket_number VARCHAR(30) NOT NULL UNIQUE, user_id UUID REFERENCES users(id), category VARCHAR(40) NOT NULL,
 subject VARCHAR(180) NOT NULL, description VARCHAR(3000) NOT NULL, priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
 status VARCHAR(30) NOT NULL DEFAULT 'OPEN', assigned_to UUID REFERENCES users(id), resolution VARCHAR(2000),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), resolved_at TIMESTAMPTZ
);
CREATE INDEX idx_support_ticket_user ON support_tickets(user_id);

CREATE TABLE operations_incidents (
 id UUID PRIMARY KEY, facility_id UUID REFERENCES facilities(id), booking_id UUID REFERENCES bookings(id), reported_by UUID NOT NULL REFERENCES users(id),
 incident_type VARCHAR(40) NOT NULL, severity VARCHAR(20) NOT NULL, description VARCHAR(3000) NOT NULL, status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
 resolution VARCHAR(2000), created_at TIMESTAMPTZ NOT NULL DEFAULT now(), resolved_at TIMESTAMPTZ
);
CREATE TABLE operations_handover (
 id UUID PRIMARY KEY, staff_user_id UUID NOT NULL REFERENCES users(id), shift_date DATE NOT NULL, notes VARCHAR(4000) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE equipment_transactions (
 id UUID PRIMARY KEY, booking_id UUID REFERENCES bookings(id), facility_id UUID REFERENCES facilities(id), customer_user_id UUID REFERENCES users(id),
 equipment_name VARCHAR(160) NOT NULL, quantity INT NOT NULL CHECK(quantity>0), issued_by UUID REFERENCES users(id), issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 returned_by UUID REFERENCES users(id), returned_at TIMESTAMPTZ, condition_out VARCHAR(300), condition_in VARCHAR(300), damage_note VARCHAR(1000)
);

CREATE TABLE user_consents (
 id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES users(id), consent_type VARCHAR(40) NOT NULL, document_version VARCHAR(40) NOT NULL,
 accepted BOOLEAN NOT NULL, accepted_at TIMESTAMPTZ NOT NULL DEFAULT now(), UNIQUE(user_id,consent_type,document_version)
);

CREATE TABLE booking_reschedules (
 id UUID PRIMARY KEY, booking_id UUID NOT NULL REFERENCES bookings(id), old_slot_id UUID NOT NULL REFERENCES booking_slots(id),
 new_slot_id UUID NOT NULL REFERENCES booking_slots(id), changed_by UUID NOT NULL REFERENCES users(id), changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
