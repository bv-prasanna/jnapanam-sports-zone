# Module 09 — Administration, Operations and Reporting

## Included

- Operations booking lookup and one-time check-in
- Check-in operator and timestamp tracking
- Admin dashboard with users, facilities, bookings, memberships, training, tournaments and revenue KPIs
- Gross revenue, processed refunds and net revenue reporting in paise
- Booking/facility participation and check-in reports
- Membership plan/status reports
- Payment reconciliation summary and missing-provider-ID detection
- Automatic audit metadata for mutating admin/operations API calls
- Restricted audit-log browsing with a safe maximum result size
- Regression coverage for check-in, reporting authorization, reconciliation and audit history

## APIs

- `POST /api/v1/operations/bookings/{bookingNumber}/check-in`
- `GET /api/v1/admin/dashboard?from=YYYY-MM-DD&to=YYYY-MM-DD`
- `GET /api/v1/admin/reconciliation`
- `GET /api/v1/admin/reports/revenue`
- `GET /api/v1/admin/reports/bookings`
- `GET /api/v1/admin/reports/memberships`
- `GET /api/v1/admin/audit-logs?limit=100`

Audit records exclude request bodies, credentials, JWTs and payment secrets.
