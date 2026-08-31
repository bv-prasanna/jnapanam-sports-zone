# Jnapanam Sports Zone Backend — Modules 01 through 05

This full replacement backend contains the completed identity foundation plus configurable sports, facilities and schedules.

## Module 05 — Training & Coaching

- Configurable sport-specific training programs, levels and age ranges
- Batches mapped to facilities, coaches, dates, times and recurring weekdays
- Capacity and fee configuration with concurrency-safe enrollment
- Customer enrollment history and cancellation
- Duplicate, age-range and full-batch rejection
- Admin/coach participant rosters and session attendance
- Program and batch activation, completion and cancellation statuses

### Training APIs

- `GET /api/v1/public/training/programs`
- `GET /api/v1/public/training/batches`
- `POST /api/v1/training/batches/{id}/enrollments`
- `GET /api/v1/training/enrollments/me`
- `POST /api/v1/training/enrollments/{id}/cancel`
- `/api/v1/admin/training/**` for program and batch management
- `/api/v1/coaching/**` for participant rosters and attendance

## Module 04 — Memberships

- Configurable plans with duration, price and daily/monthly booking limits
- Any plan can include one or more configured sports
- Public listing exposes active plans only
- Customer subscription starts as `PENDING_PAYMENT`
- Admin manual activation supports development before Module 06 payment integration
- Activation calculates the inclusive membership end date
- Suspension, reactivation, cancellation and automatic expiry
- Duplicate open subscriptions are rejected
- Customer membership history and admin subscription listing

### Membership APIs

- `GET /api/v1/public/membership-plans`
- `POST /api/v1/memberships`
- `GET /api/v1/memberships/me`
- `POST /api/v1/memberships/{id}/cancel`
- `GET/POST /api/v1/admin/memberships/plans`
- `PUT /api/v1/admin/memberships/plans/{id}`
- `PATCH /api/v1/admin/memberships/plans/{id}/status`
- `GET /api/v1/admin/memberships`
- `PATCH /api/v1/admin/memberships/{id}/status`

## Module 03 capabilities

- Generates bookable slots from each facility's weekly hours and date overrides
- Returns live remaining capacity for every slot
- Creates configurable 10-minute capacity holds before confirmation
- Uses PostgreSQL row locks to prevent overbooking under concurrent requests
- Confirms holds into bookings with human-readable booking numbers
- Releases holds and automatically cleans expired holds
- Supports customer booking history and cancellation with capacity restoration
- Provides date-filtered admin booking visibility

## Module 03 APIs

- `GET /api/v1/public/facilities/{facilityId}/slots?date=2026-09-15`
- `POST /api/v1/bookings/holds`
- `DELETE /api/v1/bookings/holds/{holdId}`
- `POST /api/v1/bookings/confirm`
- `GET /api/v1/bookings/me`
- `POST /api/v1/bookings/{bookingId}/cancel`
- `GET /api/v1/admin/bookings?date=2026-09-15`

Hold request:

```json
{"slotId":"00000000-0000-0000-0000-000000000000","quantity":1}
```

Confirmation request:

```json
{"holdId":"00000000-0000-0000-0000-000000000000"}
```

## Module 02 capabilities

- Configurable sports catalogue; seven initial sports are seed data, not hardcoded limits
- Configurable facilities and courts associated with any sport
- Default slot duration and capacity per facility
- Weekly operating hours, including fully closed days
- Full-day facility closures
- Date/time overrides for extra availability, blocked periods, capacity overrides and slot-duration overrides
- ACTIVE, TEMPORARILY_SUSPENDED and INACTIVE lifecycle for sports and facilities
- Temporary suspension dates and operational reason
- Public catalogue and schedule APIs
- Admin and operations management APIs

## Start and verify

```powershell
docker compose up -d
mvn clean test
mvn spring-boot:run
```

Open:

- Health: http://localhost:8080/actuator/health
- Swagger: http://localhost:8080/swagger-ui/index.html

Flyway applies `V3__sports_facilities_schema.sql` and inserts the initial seven sports.

## Public APIs

- `GET /api/v1/public/sports`
- `GET /api/v1/public/sports/{code}`
- `GET /api/v1/public/facilities?sportCode=BADMINTON&date=2026-09-15`
- `GET /api/v1/public/facilities/{code}`
- `GET /api/v1/public/facilities/{facilityId}/schedule?from=2026-09-01&to=2026-09-30`

## Admin APIs

Admin APIs require `SUPER_ADMIN`, `ADMIN` or `OPERATIONS`:

- Create/update/list sports and facilities
- Change sport/facility status
- Upsert weekly operating hours
- Add/remove full-day closures
- Add/remove schedule overrides

For local testing, promote an already registered user:

```powershell
docker exec jnapanam-postgres psql -U jnapanam -d jnapanam_sports_zone -c "INSERT INTO user_roles(user_id, role_id) SELECT u.id, r.id FROM users u CROSS JOIN roles r WHERE u.email='prasanna.test01@example.com' AND r.name='ADMIN' ON CONFLICT DO NOTHING;"
```

Log in again after promotion to receive a JWT containing the new role.

## Status examples

Activate:

```json
{"status":"ACTIVE","suspendedFrom":null,"suspendedUntil":null,"reason":null}
```

Temporarily suspend:

```json
{"status":"TEMPORARILY_SUSPENDED","suspendedFrom":"2026-09-10","suspendedUntil":"2026-09-20","reason":"Scheduled maintenance"}
```

Permanently deactivate:

```json
{"status":"INACTIVE","suspendedFrom":null,"suspendedUntil":null,"reason":"Service discontinued"}
```

Production must provide database credentials and a randomly generated Base64 JWT secret through environment variables.
The local development access-token lifetime is 30 days (`43200` minutes); reduce it before production deployment.

## Automated regression tests

Keep Docker Desktop running, then execute the complete Module 01 and Module 02 regression suite with one command:

```powershell
mvn clean verify
```

The suite automatically starts a disposable PostgreSQL 17 container, applies every Flyway migration and tests:

- Registration, profile, login, refresh-token rotation and logout
- Duplicate registration, invalid input and incorrect-password rejection
- Public access to the sports catalogue
- Customer rejection from protected admin APIs
- Admin sport creation, suspension and reactivation
- Facility creation, operating hours, closures and schedule overrides
- Public facility and schedule retrieval
- Slot generation, capacity holds, overbooking rejection, confirmation and cancellation
- Membership plan, subscription, activation, duplicate prevention and cancellation lifecycle
- Training program, coach/batch configuration, enrollment, capacity, attendance and cancellation

The disposable test database is removed automatically and does not modify the local development database. Test reports are written to `target/surefire-reports`.
