# Jnapanam — Page-by-page source review and UAT register (2026-11-10)

**Scope:** 38 web page modules and 23 Android route files inspected via the repository. **Static code review only**: statuses below are deliberately **UAT pending**, not verified visual/device outcomes. Record deploy SHA, screenshots and role with every result.

## Universal acceptance rules

- **Design:** check 320/375/768/1440px, portrait/landscape, readability, official logo, responsive header/footer/QR, tap targets, keyboard and screen reader.
- **Localization:** verify both English and Kannada translations with a native speaker, including validation, success/error notices, invoices and buttons.
- **Functional states:** loading, empty, success and API errors (0/offline, 400, 401, 403, 404, 409, 422, 426, 429, 5xx). No technical stack traces, stale “success” states or infinite login loops.
- **Security:** backend checks role/ownership; test direct URLs/API requests with wrong roles, expired token, duplicate submission, invalid file, PII and audit correlation ID.
- **Financial:** capture webhook and replay, failed/unknown payment, stale quote, tax/fee rounding, partial refund and no-show deduction description. Do not equate refund initiation with settlement.
- **Evidence:** test date, browser/device/OS, APK build, web/backend SHA, test actor, expected/observed result, screenshot/video, HTTP code, correlation ID and defect reference.

## Website — 38 routes

| Route | Role | Focused positive/negative test cases | Evidence |
|---|---|---|---|
| `/` | Public | Hero, nav, footer QR, brand, booking CTA, responsiveness | UAT pending |
| `/about` | Public | Official portrait/logo, media cropping, screen-reader labels | UAT pending |
| `/sports` | Public | Sports/filter availability, disabled facility, empty catalog | UAT pending |
| `/services` | Public | Service tiles, booking route, Kannada copy | UAT pending |
| `/contact` | Public | Form validation, delivery failures, focus, safe statuses | UAT pending |
| `/events` | Public | Upcoming/expired events, empty content, responsiveness | UAT pending |
| `/gallery` | Public | Images and alt text, mobile cropping, empty media | UAT pending |
| `/memberships` | Public | Plans/prices, unavailable membership | UAT pending |
| `/app` | Public | APK managed URL, no release, outdated APK, QR | UAT pending |
| `/login` | Public | Wrong password no MFA, correct password, email failure, resend | UAT pending |
| `/register` | Public | Duplicate email, bad/expired OTP, keyboard validation | UAT pending |
| `/forgot-password` | Public | Reset, expiry, no account enumeration, rate limits | UAT pending |
| `/dashboard` | Customer | Bookings, payment status, session expiry, empty data | UAT pending |
| `/profile` | Customer | Profile/photo/address proof privacy and persistence | UAT pending |
| `/account-services` | Customer | Role-appropriate services and empty states | UAT pending |
| `/billing-profile` | Customer | Tax/address validation, data ownership | UAT pending |
| `/billing-documents/[id]` | Customer | Owner-only documents, 403/404/download failure | UAT pending |
| `/subscriptions` | Customer | Mandates, renewals, failed payments | UAT pending |
| `/shop` | Customer | Cart/quantities/inventory/payment errors | UAT pending |
| `/shop-counter` | Shopkeeper | Counter sale, inventory, receipt and role checks | UAT pending |
| `/operations` | Operator | Seven-day slots, QR/check-in/no-show and counts | UAT pending |
| `/operations/front-desk` | Operator | Walk-in, access, payment state | UAT pending |
| `/admin` | Admin | Dashboard/nav, payments, pending refund vs completed | UAT pending |
| `/admin/bookings` | Admin | Cancel, no-show, deducted refunds and audit | UAT pending |
| `/admin/users` | Admin | Role escalation, search/filters, disabled users | UAT pending |
| `/admin/pricing` | Admin | Tax/pricing rounding, stale quote, invalid data | UAT pending |
| `/admin/billing` | Admin | Invoices/credit notes, delivery failure | UAT pending |
| `/admin/commerce` | Admin | Stock, catalog upload, price validation | UAT pending |
| `/admin/verification` | Admin | Private documents, role ownership and audit | UAT pending |
| `/admin/subscriptions` | Admin | Agreement management, failure/reconciliation | UAT pending |
| `/admin/operator-assignments` | Admin | Assignment boundaries and role checks | UAT pending |
| `/admin/player-growth` | Admin | Chart filters, empty state | UAT pending |
| `/admin/features` | Admin | Disabled features inaccessible directly | UAT pending |
| `/admin/notification-providers` | Super Admin | SMTP keys masked, test send≠inbox delivered | UAT pending |
| `/admin/app-management` | Super Admin | APK build/version and signed download | UAT pending |
| `/admin/uat-otp` | Super Admin | UAT-only control unavailable in production | UAT pending |
| `/admin/errors` | Admin | Correlation lookup, secrets redacted, pagination | UAT pending |
| `/admin/release-readiness` | Admin | Migration/backups/version evidence and gates | UAT pending |

## Android — 23 route files

| Route | Role | Focused positive/negative test cases | Evidence |
|---|---|---|---|
| `/` | Customer | Guest/role home, logo, language, navigation | Device UAT pending |
| `/_layout` | All | Session, update gate and navigation permissions | Device UAT pending |
| `/explore` | Customer | Facility list and offline/empty states | Device UAT pending |
| `/login` | All | Password then typed MFA, resend, wrong code, bad SMTP | Device UAT pending |
| `/register` | Customer | Email OTP request/verify, duplicate and expiry | Device UAT pending |
| `/forgot-password` | All | Reset/OTP limits and failure messages | Device UAT pending |
| `/bookings` | Customer | Hold/pay/cancel, detail scrolling, QR, refunds | Device UAT pending |
| `/booking-qr` | Customer | Ownership, expired or offline QR | Device UAT pending |
| `/checkin` | Operator | Camera permission and double check-in | Device UAT pending |
| `/scan-checkin` | Operator | Stale/wrong QR and access denial | Device UAT pending |
| `/operations` | Operator | Today counts, slots, no-show, checkout | Device UAT pending |
| `/profile` | Customer | Web profile parity, photo/address/consent | Device UAT pending |
| `/admin` | Admin | Only operational actions, hide dangerous maintenance | Device UAT pending |
| `/billing` | Customer | Invoice and tax totals, download failure | Device UAT pending |
| `/growth` | Customer | Metrics/summaries and empty state | Device UAT pending |
| `/memberships` | Customer | Eligibility, active plans, payment status | Device UAT pending |
| `/notifications` | All | Read/unread, delivery vs notification | Device UAT pending |
| `/payment-return` | Customer | Browser return, captured payment confirmation | Device UAT pending |
| `/shop-counter` | Shopkeeper | Shop counter stock/receipt, role denial | Device UAT pending |
| `/shop` | Customer | Quantity changes, inventory and checkout | Device UAT pending |
| `/subscriptions` | Customer | Mandate approve/decline, retry and cancel | Device UAT pending |
| `/tournaments` | Customer | Registration, duplicate and limits | Device UAT pending |
| `/training` | Customer | Batch capacity, enroll, payments | Device UAT pending |

## Priority UAT negative test matrix

| Trigger | Expected authoritative result |
|---|---|
| Wrong password | 401; **no MFA field**; do not send verification email |
| Correct password without MFA | 401 with MFA_REQUIRED; email dispatched/acknowledged, not guaranteed inbox delivery |
| Invalid/expired MFA | 401 MFA_INVALID; allow re-request without login loop |
| Mail provider outage | 503 MFA_DELIVERY_FAILED; do not claim email was sent |
| Forged X-Forwarded-For under repeated login | 429 AUTH_RATE_LIMITED after configured threshold; Retry-After + support reference |
| Final slot booked concurrently | No overbooking; consistent 409 or business conflict |
| Payment success notification but gateway failure | Booking remains unpaid/pending, not confirmed paid |
| Refund deducted amount + description | Net refund correct, visible ledger; provider confirmation before completion |
| Provider timeout during refund | Uncertain state and reconciliation, not blind retry |
| Stale/wrong-owner QR or wrong role | 403/409; no check-in or data leak |
| Disabled Android release | 426 with functional upgrade path |
| Wrong-role invoice or ID document | Forbidden/not found without customer PII |

## Gate

CI must pass at the **exact release SHA**. Finance owner signs off Razorpay sandbox capture/refund, operations owner signs off check-in and no-show, venue owner approves UI/Kannada, and DBA signs off real staging backup/restore and Flyway history. No production certification until evidence is attached.
