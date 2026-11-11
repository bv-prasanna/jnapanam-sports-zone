# Jnapanam — HTTP status, logging, security and per-page UAT checklist

> Source-only assessment on main, 2026-11-10. This is an acceptance-test specification, **not** evidence that every page has been rendered or clicked on a deployed device.

## API response contract

The 2026-11-10 hardening PR adds typed Android MFA errors and prevents bypass of login throttling using untrusted X-Forwarded-For. Throttling now returns a stable AUTH_RATE_LIMITED code, Retry-After and available requestId (from the outer telemetry filter). This is **in-process**, not a distributed/WAF solution. Review [security findings](SECURITY_REVIEW_2026-11-10.md) and [route register](PAGE_BY_PAGE_UAT_2026-11-10.md).



Errors should use RFC 9457-style Spring ProblemDetail: HTTP status; safe detail; stable property code; requestId; response header X-Correlation-ID. Clients must use structured codes rather than guessing from message text.

| HTTP | Meaning | Expected UI |
|---|---|---|
| 200/201 | Verified read/create | Success only after confirmed API response |
| 202 | Accepted async operation | Show pending, not completed |
| 204 | Completed without body | No JSON parsing failure |
| 400/422 | Validation/rule violation | Field-level corrections, retain form input |
| 401 | Not authenticated / invalid credentials / MFA | Differentiate MFA_REQUIRED and MFA_INVALID from expired session |
| 403 | Forbidden | Keep session; show access denied |
| 404 | Missing resource / removed booking | Show empty/not found, refresh link |
| 409 | State conflict (slot taken, duplicate, refunded) | Explain state conflict; offer refresh |
| 426 | Unsupported Android version | Prompt mandatory update, no login loop |
| 429 | Rate limit | Wait/retry notice; avoid instant resubmission |
| 500 | Unexpected server failure | Generic message and support reference |
| 502/503/504 | Provider/network unavailable | Pending/unknown state where appropriate; no duplicate payment/refund retry without verification |

Specific tested handlers: MFA_DELIVERY_FAILED (503), MFA_INVALID (401), MFA_REQUIRED (401), VALIDATION_FAILED (400), REFUND_STATUS_UNCERTAIN (503). Refund uncertainty must say **reconcile before retrying**. The backend logs class and correlation reference, not raw exception message.

## Audit evidence

- Filter captures HTTP method, sanitized path, response status, request ID and elapsed time; mutation and failed-request events are recorded.
- Audit source is apps/backend/src/main/java/com/jnapanam/sportszone/admin/web; admin viewing is apps/web/app/admin/errors/page.tsx.
- The request cannot pick an arbitrary audit IP by sending X-Forwarded-For. Capture trusted edge IP separately at a configured ingress, or accept that getRemoteAddr may identify the reverse proxy.
- Audit geolocation headers are **client-reported**, consent-dependent hints, not verified position.
- Never log passwords, OTP, tokens, payment card data, customer address proofs or exception strings that might contain secrets.

## Role-by-role screens

| Surface | Key routes in source | Required checks |
|---|---|---|
| Public web | /, /about, /sports, /services, /events, /gallery, /contact, /shop | Responsive header/footer, images, SEO, Kannada, empty/failed API, keyboard navigation |
| Authentication | /register, /login, /forgot-password | Invalid password, MFA failure, resend limits, non-disclosure, correct status text |
| Customer | /dashboard, /profile, /memberships, /subscriptions, /billing-profile, /billing-documents/[id] | Owner access, empty states, profile updates, invoices, token refresh |
| Admin | /admin and subroutes for bookings/users/errors/pricing/billing/verification/subscriptions/features/app-management/release-readiness | Granular RBAC, empty-state, confirmation, error rendering, audit ID, mobile viewport |
| Operations | /operations, /operations/front-desk, /shop-counter | Today/tomorrow selection, check-in evidence, capacity, cancellation/no-show, onsite paid vs pending |
| Mobile | Expo routes for login/register/profile, bookings, payment-return, notifications, operations, checkin/scan-checkin, membership, training, tournaments, subscriptions, shop, admin | Small-screen fit, safe area, back behavior, 401/403/426, Kannada, offline retry, deep-link return |

Full routes are in apps/web/app/**/page.tsx and apps/mobile/src/app/*.tsx. The admin error page already supports status filtering and reference copies; real-device copy-feedback and accessibility still require QA.

## Negative security / finance tests

1. Customer A requests customer B's booking, payment, invoice, profile photo and address proof: must be 403/404 without data leakage.
2. Operator assigned to Facility A cannot edit Facility B, including through direct API calls.
3. Wrong password must not move the user into MFA. Correct password with failed MFA delivery must return 503, not pretend the challenge was sent.
4. Tampered/expired JWT, stale refresh token, concurrent refresh requests, disallowed CORS origin and replayed OTP.
5. Two users simultaneously reserve final slot; only one successful capacity transition, no negative counters.
6. Existing hold expires between quote and confirmation: return 409; do not charge twice.
7. Browser returns from Razorpay without a verified capture: show pending/failed, not successful.
8. Duplicate signed webhooks and modified signature: idempotent handling/rejection.
9. Refund deduction greater than captured amount, negative deduction, missing explanation, double refund, uncertain provider timeout and partial no-show refund.
10. Cancellation and no-show followed by pending refund: booking, payment status and notification cannot falsely indicate refunded.
11. Payment success but notification provider down: booking stays paid; notification retry is queued, not duplicate payment.
12. Offline mobile login, deep-link app return, server 500, HTTP 204 empty body, 426 minimum app version, and slow network do not cause navigation loops.
13. Public route must not expose privileged admin data; role removed mid-session must deny next server operation.
14. Audit reference IDs stay valid and do not expose query strings, passwords, OTP, card details or bearer tokens.

## UI design acceptance

Consistent typography and spacing tokens; branded logo; accessible contrast; 44px-ish touch targets on mobile; visible focus; descriptive button text; busy/disabled states; confirmation for irreversible changes; field-level errors; success/pending/failure distinction; copyable support reference; pagination and skeleton/empty states for large tables. Kannada/English parity is a release gate, not a decorative translation requirement.

**No UAT completion claim without screenshots and interaction logs from web widths 375/390/768/1440 and signed Android APK on physical devices.** Capture known defects and resolution commits in the release issue.
