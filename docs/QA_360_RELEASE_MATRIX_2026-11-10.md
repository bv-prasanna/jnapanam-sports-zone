# Jnapanam 360-degree UI, security, statuses and negative-case release matrix

**Status:** Audit plan and repository-based findings; not a claim that every screen has been exercised. Run all cases on web and physical Android with real staging APIs. Prioritize payment/refund, session/MFA and booking consistency.

| Journey | Happy path | Negative/security cases | Expected user status / evidence |
|---|---|---|---|
| Login / MFA | password then verified MFA | wrong password must not trigger MFA, expired OTP, brute-force, revoked token, replay | 401 generic invalid credentials; 429 throttled; no OTP/token in logs |
| Registration / reset | verified mobile/email | duplicate, invalid contact, OTP expiry, resend flood | 400/409 validation, 429 limit; no account activation without verification |
| Sports / slots | browse availability | closed facility, overlapping periods, invalid dates, midnight boundary, timezone | 400/409; no phantom slots or 500 |
| Booking | reserve and confirm | double-book, concurrent capacity, retry/idempotency, abandoned checkout | 409 conflict; pending payment not confirmed |
| Razorpay | signed capture and webhook | tampered amount, invalid signature, duplicate/out-of-order webhook, network timeout | only server-confirmed payment shown successful |
| Cancellation/refund | valid future cancellation, partial deduction | past no-show exception, over-refund, negative deduction, duplicate request, provider rejection | separate cancellation, initiated refund, processed refund states |
| QR attendance | valid one-time check-in | screenshot/replay, wrong facility, wrong date, role bypass | 403/409; no duplicate attendance |
| Operator | today/tomorrow/day-after, periods | wrong facility, empty state, stale counts, expired session | role-scoped data and clear empty/error states |
| Shop | order and onsite collection | unpaid, duplicate collection, negative stock, forged staff role | persisted payment status only |
| Billing / tax | GST invoice and receipt | incorrect rate, cross-state tax, rounding, missing customer details | immutable financial snapshots; audit trail |
| Membership / training / tournaments | enrollment, schedules | capacity, duplicate enrollment, expired membership | 409 or actionable validation |
| Notifications | delivery after confirmed event | provider outage, retry, duplicate notification, failed payment | no false payment success; delivery state distinct |
| Admin | facility, user, role, pricing, tax | unauthorized access, invalid inputs, CSRF/CORS, audit deletion | 401/403/422 and immutable audit evidence |
| Mobile | app login, navigation, update | offline, session expiry, old APK, back navigation, permissions | readable localized errors; no raw fetch stack |
| Web UI | every route and responsive state | 360/390/768/1280px, keyboard-only, contrast, missing logo/image | no overflow, visible focus, clear loading/empty/error/success |
| Logs | correlation IDs, operational events | PII, secrets, card details, OTP, log injection | structured redacted logs and traceable support reference |

## Status and HTTP contract
- 200/201: persisted success; 202: accepted/pending (never label paid/refunded); 204: success with no body.
- 400/422: actionable input validation; 401: authentication required/expired; 403: insufficient permission; 404: resource unavailable or inaccessible; 409: stale state, duplicate or capacity conflict; 429: retry later; 5xx: generic safe error plus correlation ID.
- Network timeout or 502 does **not** prove payment or booking failure: reconcile provider/backend before retry.
- Frontend and mobile must not show raw Java exception, SQL, internal host, token, stack trace or technical fetch failure.
- Log request ID, event name, actor ID (not secret), resource ID, outcome, duration and exception category; redact mobile/email where possible.

## Exit criteria
1. CI green on the candidate SHA: backend tests, web build, Android build and security tests.
2. Automated tests cover all critical negative cases and at least one real-device run for each role.
3. Finance reconciliation: captured/failed/pending payments, partial refunds, credit notes and invoice totals.
4. Deployment/backup/restore and notification-provider verification signed off.
5. Zero open P0/P1 defects, reviewed P2/P3 exceptions, reproducible screenshots/log references, customer approval.

## Current known follow-ups
- Existing `docs/STATUS_MESSAGES_AUDIT.md` defines user-facing state contracts; validate them against current screens and provider events.
- Existing `docs/UAT_TO_PRODUCTION_GATE.md` warns about test OTP and unsigned debug APKs; re-check current production settings before release.
- GitHub CI for PR #87 failed on 2026-11-09. Diagnose it rather than assuming all pending PRs are ready.
- `docs/architecture-kt/README.md` and `SCHEMA_INDEX.md` already provide migration inventory and ER notes. These new docs supplement, not replace, them.
