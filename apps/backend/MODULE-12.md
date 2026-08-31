# Module 12 — Reports, Audit and Backend Stabilization

## Included

- Operational dashboard and payment reconciliation retained from Module 9
- Revenue, booking and membership reports retained and hardened
- Facility/court capacity-utilization report
- Customer activity summary report
- Training batch enrollment and attendance report
- Tournament team and fixture progress report
- CSV download for every report, with safe escaping
- Maximum two-year date range validation
- Searchable audit trail by actor, action, resource, result and time range
- Authentication success/failure, token refresh, logout and password-change audit records
- Correlation ID returned to clients and persisted for support investigation
- Resource type and resource ID extraction for admin mutations
- Response status, success flag, duration, IP address and user-agent capture
- Database trigger preventing audit-log updates and deletions
- Role-protected reporting and audit APIs
- Module 12 integration regression test

## APIs

- `GET /api/v1/admin/dashboard`
- `GET /api/v1/admin/reconciliation`
- `GET /api/v1/admin/reports/revenue`
- `GET /api/v1/admin/reports/bookings`
- `GET /api/v1/admin/reports/memberships`
- `GET /api/v1/admin/reports/utilization`
- `GET /api/v1/admin/reports/customers`
- `GET /api/v1/admin/reports/training`
- `GET /api/v1/admin/reports/tournaments`
- `GET /api/v1/admin/reports/{report}/export?format=csv`
- `GET /api/v1/admin/audit-logs`
- `GET /api/v1/admin/audit-logs/search`

The audit table is append-only at the database level. Reports and audit endpoints require
`SUPER_ADMIN`, `ADMIN` or `OPERATIONS`.

## Validation

Keep Docker Desktop running and execute:

```powershell
mvn clean verify
```

Expected result: 18 tests with zero failures and zero errors.
