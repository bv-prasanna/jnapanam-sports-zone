# Module 13 — Backend Completion and Operational Workflows

## Included

- Payment-gated paid bookings, training enrollments and tournament teams
- Zero-priced offerings remain immediately confirmed for backward compatibility
- Successful payment fulfils all four purposes: booking, membership, training and tournament
- Configurable 15-minute payment window
- Scheduled expiry of unpaid records and automatic booking-capacity release
- Re-registration after an expired/cancelled training or tournament payment
- Operations booking cancellation, no-show, check-in reversal, and combined cancellation/refund
- Secure one-time Super Admin bootstrap through environment variables
- Forgot/reset-password token lifecycle, single use, 15-minute expiry and refresh-token revocation
- Non-disclosing forgot-password responses
- Login/reset endpoint rate limiting
- Module 12 nullable audit-search SQL correction included
- Updated regression automation for paid training, paid tournaments and password recovery

## New states

- Booking: `PENDING_PAYMENT`, `CONFIRMED`, `CANCELLED`, `PAYMENT_EXPIRED`, `NO_SHOW`
- Training: `PENDING_PAYMENT`, `ENROLLED`, `CANCELLED`, `PAYMENT_EXPIRED`
- Tournament team: `PENDING_PAYMENT`, `REGISTERED`, `WITHDRAWN`, `DISQUALIFIED`, `PAYMENT_EXPIRED`

## New APIs

- `POST /api/v1/auth/password/forgot`
- `POST /api/v1/auth/password/reset`
- `POST /api/v1/operations/bookings/{id}/cancel`
- `POST /api/v1/operations/bookings/{id}/cancel-and-refund`
- `POST /api/v1/operations/bookings/{id}/no-show`
- `POST /api/v1/operations/bookings/{id}/check-in/reverse`

## First Super Admin

Set these only when the database does not yet contain a Super Admin:

```text
BOOTSTRAP_SUPER_ADMIN_EMAIL=admin@jnapanamsportszone.in
BOOTSTRAP_SUPER_ADMIN_PASSWORD=replace-with-a-strong-temporary-password
BOOTSTRAP_SUPER_ADMIN_NAME=Jnapanam Super Admin
```

The bootstrap is skipped permanently after the first Super Admin exists. The
temporary password must be changed at first login.

## Password recovery

`EXPOSE_DEVELOPMENT_RESET_TOKEN=true` returns the reset token for local testing.
Set it to `false` in production and deliver the token using the configured
email/SMS provider.

## Verification

```powershell
mvn clean verify
```

Expected result: 19 tests, zero failures and zero errors.
