# Jnapanam — Screen-by-screen UAT execution matrix

> Generated from GitHub `main` file inventory on 2026-11-10. **This matrix is a test plan, not a claim that a visual or device test passed.** It lists 38 web route page files and 22 mobile screen files. Test all routes using customer, operator, shopkeeper and admin accounts with different permissions.

## Execution protocol

1. Record environment and exact Git SHA; use isolated UAT database, synthetic accounts and Razorpay test mode.
2. For each route, capture 375/390/768/1440 px screenshots on web and Android physical-device screenshots where applicable. Check consistent brand/logo, hierarchy, navigation, focus, touch area, readable labels, Kannada/English parity, and no horizontal overflow.
3. Execute healthy API, empty dataset, slow request, offline/network failure, expired session, unauthorized role, 404, 409 conflict, 429 throttle and 503/500; do not confuse missing data with failed load.
4. For every write, test double-click, timeout after commit, retry, conflict, concurrent actor, stale version, and status-to-server reconciliation. Never claim refund/payment success from request initiation.
5. Record evidence link, API response/status, correlation ID, release SHA, screen/device and owner. Mark PASS only with evidence.

## Website routes

| Route | Implementation file | Required access (review target) | Negative / UX focus | Result |
|---|---|---|---|---|
| `/about` | `apps/web/app/about/page.tsx` | Public, subject to feature permissions | Loading, empty, 401/403/500, keyboard/focus, EN/KN text, responsive layout | Not executed |
| `/account-services` | `apps/web/app/account-services/page.tsx` | Authenticated customer / authorized role | Loading, empty, 401/403/500, keyboard/focus, EN/KN text, responsive layout | Not executed |
| `/admin/app-management` | `apps/web/app/admin/app-management/page.tsx` | ADMIN / SUPER_ADMIN | Unauthorized role, failed fetch vs no records, bulk actions, tables 375px, confirm/destructive action | Not executed |
| `/admin/billing` | `apps/web/app/admin/billing/page.tsx` | ADMIN / SUPER_ADMIN | Unauthorized role, failed fetch vs no records, bulk actions, tables 375px, confirm/destructive action | Not executed |
| `/admin/bookings` | `apps/web/app/admin/bookings/page.tsx` | ADMIN / SUPER_ADMIN | Unauthorized role, failed fetch vs no records, bulk actions, tables 375px, confirm/destructive action | Not executed |
| `/admin/commerce` | `apps/web/app/admin/commerce/page.tsx` | ADMIN / SUPER_ADMIN | Unauthorized role, failed fetch vs no records, bulk actions, tables 375px, confirm/destructive action | Not executed |
| `/admin/errors` | `apps/web/app/admin/errors/page.tsx` | ADMIN / SUPER_ADMIN | Unauthorized role, failed fetch vs no records, bulk actions, tables 375px, confirm/destructive action | Not executed |
| `/admin/features` | `apps/web/app/admin/features/page.tsx` | ADMIN / SUPER_ADMIN | Unauthorized role, failed fetch vs no records, bulk actions, tables 375px, confirm/destructive action | Not executed |
| `/admin/notification-providers` | `apps/web/app/admin/notification-providers/page.tsx` | ADMIN / SUPER_ADMIN | Unauthorized role, failed fetch vs no records, bulk actions, tables 375px, confirm/destructive action | Not executed |
| `/admin/operator-assignments` | `apps/web/app/admin/operator-assignments/page.tsx` | ADMIN / SUPER_ADMIN | Unauthorized role, failed fetch vs no records, bulk actions, tables 375px, confirm/destructive action | Not executed |
| `/admin` | `apps/web/app/admin/page.tsx` | ADMIN / SUPER_ADMIN | Unauthorized role, failed fetch vs no records, bulk actions, tables 375px, confirm/destructive action | Not executed |
| `/admin/player-growth` | `apps/web/app/admin/player-growth/page.tsx` | ADMIN / SUPER_ADMIN | Unauthorized role, failed fetch vs no records, bulk actions, tables 375px, confirm/destructive action | Not executed |
| `/admin/pricing` | `apps/web/app/admin/pricing/page.tsx` | ADMIN / SUPER_ADMIN | Unauthorized role, failed fetch vs no records, bulk actions, tables 375px, confirm/destructive action | Not executed |
| `/admin/release-readiness` | `apps/web/app/admin/release-readiness/page.tsx` | ADMIN / SUPER_ADMIN | Unauthorized role, failed fetch vs no records, bulk actions, tables 375px, confirm/destructive action | Not executed |
| `/admin/subscriptions` | `apps/web/app/admin/subscriptions/page.tsx` | ADMIN / SUPER_ADMIN | Unauthorized role, failed fetch vs no records, bulk actions, tables 375px, confirm/destructive action | Not executed |
| `/admin/uat-otp` | `apps/web/app/admin/uat-otp/page.tsx` | ADMIN / SUPER_ADMIN | Unauthorized role, failed fetch vs no records, bulk actions, tables 375px, confirm/destructive action | Not executed |
| `/admin/users` | `apps/web/app/admin/users/page.tsx` | ADMIN / SUPER_ADMIN | Unauthorized role, failed fetch vs no records, bulk actions, tables 375px, confirm/destructive action | Not executed |
| `/admin/verification` | `apps/web/app/admin/verification/page.tsx` | ADMIN / SUPER_ADMIN | Unauthorized role, failed fetch vs no records, bulk actions, tables 375px, confirm/destructive action | Not executed |
| `/app` | `apps/web/app/app/page.tsx` | Public, subject to feature permissions | Loading, empty, 401/403/500, keyboard/focus, EN/KN text, responsive layout | Not executed |
| `/billing-documents/[id]` | `apps/web/app/billing-documents/[id]/page.tsx` | Authenticated customer / authorized role | Unverified payment, duplicate webhook, 503 uncertain refund, tax totals, download authorization | Not executed |
| `/billing-profile` | `apps/web/app/billing-profile/page.tsx` | Authenticated customer / authorized role | Unverified payment, duplicate webhook, 503 uncertain refund, tax totals, download authorization | Not executed |
| `/contact` | `apps/web/app/contact/page.tsx` | Public, subject to feature permissions | Loading, empty, 401/403/500, keyboard/focus, EN/KN text, responsive layout | Not executed |
| `/dashboard` | `apps/web/app/dashboard/page.tsx` | Authenticated customer / authorized role | Loading, empty, 401/403/500, keyboard/focus, EN/KN text, responsive layout | Not executed |
| `/events` | `apps/web/app/events/page.tsx` | Public, subject to feature permissions | Loading, empty, 401/403/500, keyboard/focus, EN/KN text, responsive layout | Not executed |
| `/forgot-password` | `apps/web/app/forgot-password/page.tsx` | Guest | Wrong password, expired/invalid OTP, email failure, rate limit, no MFA on wrong password | Not executed |
| `/gallery` | `apps/web/app/gallery/page.tsx` | Public, subject to feature permissions | Loading, empty, 401/403/500, keyboard/focus, EN/KN text, responsive layout | Not executed |
| `/login` | `apps/web/app/login/page.tsx` | Guest | Wrong password, expired/invalid OTP, email failure, rate limit, no MFA on wrong password | Not executed |
| `/memberships` | `apps/web/app/memberships/page.tsx` | Authenticated customer / authorized role | Loading, empty, 401/403/500, keyboard/focus, EN/KN text, responsive layout | Not executed |
| `/operations/front-desk` | `apps/web/app/operations/front-desk/page.tsx` | Assigned operator / staff | Facility RBAC, offline/slow network, double action, stale state, role-specific navigation | Not executed |
| `/operations` | `apps/web/app/operations/page.tsx` | Assigned operator / staff | Facility RBAC, offline/slow network, double action, stale state, role-specific navigation | Not executed |
| `/` | `apps/web/app/page.tsx` | Public, subject to feature permissions | Loading, empty, 401/403/500, keyboard/focus, EN/KN text, responsive layout | Not executed |
| `/profile` | `apps/web/app/profile/page.tsx` | Authenticated customer / authorized role | Loading, empty, 401/403/500, keyboard/focus, EN/KN text, responsive layout | Not executed |
| `/register` | `apps/web/app/register/page.tsx` | Guest | Wrong password, expired/invalid OTP, email failure, rate limit, no MFA on wrong password | Not executed |
| `/services` | `apps/web/app/services/page.tsx` | Public, subject to feature permissions | Loading, empty, 401/403/500, keyboard/focus, EN/KN text, responsive layout | Not executed |
| `/shop-counter` | `apps/web/app/shop-counter/page.tsx` | Assigned operator / staff | Facility RBAC, offline/slow network, double action, stale state, role-specific navigation | Not executed |
| `/shop` | `apps/web/app/shop/page.tsx` | Public, subject to feature permissions | Loading, empty, 401/403/500, keyboard/focus, EN/KN text, responsive layout | Not executed |
| `/sports` | `apps/web/app/sports/page.tsx` | Public, subject to feature permissions | Loading, empty, 401/403/500, keyboard/focus, EN/KN text, responsive layout | Not executed |
| `/subscriptions` | `apps/web/app/subscriptions/page.tsx` | Authenticated customer / authorized role | Unverified payment, duplicate webhook, 503 uncertain refund, tax totals, download authorization | Not executed |

## Expo mobile routes

| Route | Implementation file | Required access (review target) | Negative / UX focus | Result |
|---|---|---|---|---|
| `/admin` | `apps/mobile/src/app/admin.tsx` | ADMIN / SUPER_ADMIN | Unauthorized role, failed fetch vs no records, bulk actions, tables 375px, confirm/destructive action | Not executed |
| `/billing` | `apps/mobile/src/app/billing.tsx` | Authenticated customer / authorized role | Unverified payment, duplicate webhook, 503 uncertain refund, tax totals, download authorization | Not executed |
| `/booking-qr` | `apps/mobile/src/app/booking-qr.tsx` | Authenticated customer / authorized role | Sold-out, duplicate checkout, payment pending, booking cancellation/no-show, QR reuse, time boundary | Not executed |
| `/bookings` | `apps/mobile/src/app/bookings.tsx` | Authenticated customer / authorized role | Sold-out, duplicate checkout, payment pending, booking cancellation/no-show, QR reuse, time boundary | Not executed |
| `/checkin` | `apps/mobile/src/app/checkin.tsx` | Assigned operator / staff | Facility RBAC, offline/slow network, double action, stale state, role-specific navigation | Not executed |
| `/explore` | `apps/mobile/src/app/explore.tsx` | Public, subject to feature permissions | Loading, empty, 401/403/500, keyboard/focus, EN/KN text, responsive layout | Not executed |
| `/forgot-password` | `apps/mobile/src/app/forgot-password.tsx` | Guest | Wrong password, expired/invalid OTP, email failure, rate limit, no MFA on wrong password | Not executed |
| `/growth` | `apps/mobile/src/app/growth.tsx` | Authenticated customer / authorized role | Loading, empty, 401/403/500, keyboard/focus, EN/KN text, responsive layout | Not executed |
| `/index` | `apps/mobile/src/app/index.tsx` | Public, subject to feature permissions | Loading, empty, 401/403/500, keyboard/focus, EN/KN text, responsive layout | Not executed |
| `/login` | `apps/mobile/src/app/login.tsx` | Guest | Wrong password, expired/invalid OTP, email failure, rate limit, no MFA on wrong password | Not executed |
| `/memberships` | `apps/mobile/src/app/memberships.tsx` | Authenticated customer / authorized role | Loading, empty, 401/403/500, keyboard/focus, EN/KN text, responsive layout | Not executed |
| `/notifications` | `apps/mobile/src/app/notifications.tsx` | Authenticated customer / authorized role | Loading, empty, 401/403/500, keyboard/focus, EN/KN text, responsive layout | Not executed |
| `/operations` | `apps/mobile/src/app/operations.tsx` | Assigned operator / staff | Facility RBAC, offline/slow network, double action, stale state, role-specific navigation | Not executed |
| `/payment-return` | `apps/mobile/src/app/payment-return.tsx` | Authenticated customer / authorized role | Unverified payment, duplicate webhook, 503 uncertain refund, tax totals, download authorization | Not executed |
| `/profile` | `apps/mobile/src/app/profile.tsx` | Authenticated customer / authorized role | Loading, empty, 401/403/500, keyboard/focus, EN/KN text, responsive layout | Not executed |
| `/register` | `apps/mobile/src/app/register.tsx` | Guest | Wrong password, expired/invalid OTP, email failure, rate limit, no MFA on wrong password | Not executed |
| `/scan-checkin` | `apps/mobile/src/app/scan-checkin.tsx` | Assigned operator / staff | Facility RBAC, offline/slow network, double action, stale state, role-specific navigation | Not executed |
| `/shop-counter` | `apps/mobile/src/app/shop-counter.tsx` | Assigned operator / staff | Facility RBAC, offline/slow network, double action, stale state, role-specific navigation | Not executed |
| `/shop` | `apps/mobile/src/app/shop.tsx` | Public, subject to feature permissions | Loading, empty, 401/403/500, keyboard/focus, EN/KN text, responsive layout | Not executed |
| `/subscriptions` | `apps/mobile/src/app/subscriptions.tsx` | Authenticated customer / authorized role | Unverified payment, duplicate webhook, 503 uncertain refund, tax totals, download authorization | Not executed |
| `/tournaments` | `apps/mobile/src/app/tournaments.tsx` | Public, subject to feature permissions | Loading, empty, 401/403/500, keyboard/focus, EN/KN text, responsive layout | Not executed |
| `/training` | `apps/mobile/src/app/training.tsx` | Public, subject to feature permissions | Loading, empty, 401/403/500, keyboard/focus, EN/KN text, responsive layout | Not executed |

## Status-message cases (execute per affected route)

| ID | Trigger | Correct outcome |
|---|---|---|
| S-01 | Incorrect credentials | Invalid credentials; do not prompt for MFA |
| S-02 | Correct credentials, MFA email provider fails | Email delivery failed, no fictional sent-code success |
| S-03 | Invalid/expired MFA code | Retry/resend guidance, preserve identity entry |
| S-04 | Payment provider browser callback without server verification | Payment unconfirmed, offer check status |
| S-05 | Booking confirmed, payment due at venue | Confirm reservation only; show amount due |
| S-06 | Refund submission times out / backend `REFUND_STATUS_UNCERTAIN` | State unknown; reconcile with Razorpay *before* another attempt |
| S-07 | Refund accepted asynchronously | Refund initiated/pending, not refunded |
| S-08 | Operator check-in API succeeds but refresh fails | Action saved; cannot refresh list |
| S-09 | API 401/403/404/409/429/500 | Distinguish session, permissions, missing object, conflict, rate limit, safe server error |
| S-10 | Android 426 mandatory update | Update guidance; no login loop |

## Finance and authorization negative cases

- Two customers attempt final slot simultaneously; verify single bookable outcome and consistent counters.
- Customer A cannot view booking, invoice, address proof, payment, profile data of Customer B through direct API.
- Operator assigned to one facility cannot check in, refund or change another facility's booking.
- Replay a valid QR after check-in, present future-day QR, and submit mutated HMAC.
- Replayed Razorpay webhook, wrong signature, delayed capture, provider timeout, refund deduction exceeding paid amount, duplicate partial refund.
- Offline after writing booking or collecting payment: refresh/reconcile using server record; do not create another charge.
- Upload oversized or invalid profile/document file; no PII in logs, telemetry or downloaded report.
- Try admin routes with no JWT, wrong JWT, expired JWT, removed role and direct URL.
- Locale switch preserves workflow state and validated field/error labels.

## UAT evidence ledger

| Case/route | Release SHA | Browser/device | User role | Expected | Actual | Screenshot/log reference | Severity | Owner | Status |
|---|---|---|---|---|---|---|---|---|---|
| Example: `/admin/bookings` | To capture | Android/Chrome | ADMIN | From date not after To date | Not executed | — | — | — | NOT RUN |

**Release acceptance:** No open P0/P1 defects; required access/finance flows verified with actual server reconciliation; CI green; verified backup restore; live provider test-mode checks; verified Android signed APK; customer approval. See `docs/architecture-kt/README.md` and `docs/UAT_TO_PRODUCTION_GATE.md`.
