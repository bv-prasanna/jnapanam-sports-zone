create table operator_facility_assignments (
    operator_id uuid not null references users(id) on delete cascade,
    facility_id uuid not null references facilities(id) on delete cascade,
    assigned_at timestamptz not null default now(),
    primary key (operator_id, facility_id)
);

create index idx_operator_facility_assignments_facility
    on operator_facility_assignments(facility_id);
