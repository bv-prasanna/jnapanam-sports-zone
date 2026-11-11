# Jnapanam Sports Zone — Current Release Scope Freeze & Quality Gate

**Scope freeze:** 1 November 2026  
**Status:** Release candidate work in PR #64. **Not production-certified.**  
**Principle:** Only defect fixes, security corrections, accessibility/responsive fixes, tests, and deployment compatibility changes are permitted in this release branch. All new business features and additions go to the next release backlog.

## Frozen functional scope

1. **Identity:** registration, email OTP/MFA, JWT sessions, roles, password recovery/change, customer/admin/operator restrictions.
2. **Arena catalog:** site configuration, sport and facility management, operational schedules, closures, slot availability, suspension status.
3. **Customer:** slot search/quote, holds/confirmations/cancellations, saved bookings, memberships, coaching/training, tournaments, payment history, self-service billing profile, PDF receipts and issued invoices.
4. **Staff/operator:** booking verification, onsite collections, check-in/checkout, admin approvals, facility management and shop counter.
5. **Commerce and engagement:** products/shop, existing offers/coupon redemption and loyalty ledger; no opaque individual or device-based surcharges.
6. **Billing:** taxes and peak/off-peak rates by site/sport/facility/slot, server-authoritative quote, price freeze, web/mobile final-price display, checkout/onsite captured amounts, refunds, GST document issuance when legally configured, credit notes, PDF download, opt-in invoice email **disabled by default**.
7. **Administration:** responsive web tax/pricing rule editor, merchant GST configuration as incomplete draft, purpose tax policies and per-service auto-invoice preferences. Internal codes/IDs should not be exposed when a readable selector exists.
8. **Localization, accessibility and resilience:** English/Kannada where previously supported, keyboard focus states, screen-reader names and labels, mobile layouts, recoverable error states and reasonable response time under low-cost hosting.

## Known intentional defaults / prerequisites

- Supplier defaults: **Jnapanam Sports Zone**, Karnataka state code **29**. Unknown GSTIN, service code, registered address, legal signatory and GST percentages remain **unconfigured**; no fabricated values.
- Invoice email delivery: **OFF** by default, including pending retry deliveries. Customers retrieve documents from their authenticated account.
- Automatic GST issuance: **OFF** until merchant/accountant approves the purpose, tax rate, place of supply, registration, invoice timing and recipient billing details. Customer-requested invoices are allowed only after those policies are valid.
- **E-invoicing / IRN**, when legally applicable: software must block incompatible direct issuance. External IRP connection, credentials and legal applicability confirmation are prerequisites; never claim IRP integration is finished without a provider.
- Razorpay end-to-end verification is expressly excluded from this release acceptance by project owner, but offline/mock unit and integration checks still apply.
- Existing Northflank/Cloudflare production is unchanged by the draft PR. Do not merge/deploy until the following gates pass.

## Release gates — do not equate build success with UAT

| Gate | Evidence / acceptance |
| --- | --- |
| CI compilation + static checks | Latest exact PR/head SHA: backend Maven tests, web build/tests and mobile TypeScript typecheck all green |
| Database | Backed-up staging PostgreSQL successfully migrates Flyway V48–V54; restart idempotency; old bookings/invoices readable |
| Identity + RBAC | Admin/operator/customer positive and negative tests; no cross-user payment, booking or PDF access; OTP/password reset, device/session behaviour |
| Catalog and schedules | Sports/facilities CRUD, suspension and return to active, closures, slot creation and location; no manual internal identifier required in UI |
| Pricing | Site/sport/facility/slot precedence, peak/off-peak weekdays and dates, tax-inclusive/exclusive, duplicate/conflicting rules, time-window boundaries, zero-price and invalid input |
| Customer booking | Web and Android list the same available slots and final tax-inclusive customer price; batch quotes load promptly; confirm rejects changed price; cancellation and retry safe |
| Operator | Check-in/check-out verification, cash/onsite reconciliation, permission restrictions, booking lookup and accessible responsive actions |
| Membership/training/tournament | Enrollment, payment, cancellation, availability, membership benefits and notification state transitions tested |
| Promotions and loyalty | Eligibility, capped discounts, idempotency, points redemption/earn/reversal, full and partial refund accounting; no negative balances |
| Billing | Owner-only payment history/receipts, ordinary receipt even without GST profile, GST invoice when valid policy exists, paid vs refunded safeguards, PDF invoice & credit note, amount/tax reconciliation |
| No-email policy | Setting disabled => **no SMTP sends**, including earlier queued records; enabling later is an explicit admin action |
| UI/UX/accessibility | Narrow-width web/mobile review, keyboard/VoiceOver/TalkBack, readability, loading/empty/error states, native navigation, Kannada/English checks where existing |
| Infrastructure | Staging Northflank health/readiness and Cloudflare frontend API/CORS/asset checks, secrets rotation and least-privilege settings |
| Business sign-off | Operator/admin/customer UAT, tax/legal registration/accountant review and explicit production release approval |

## Release acceptance scenarios (manual browser + physical Android required)

1. Login as customer, select sport/court/date. See available slots and **the same quoted total** on web and mobile. Refresh or change pricing rule during checkout: confirm should reject stale amount without charging.
2. Create site-wide peak surcharge, then targeted court/slot override. Verify the most-specific matching rule wins, dates/weekday controls apply and unchanged bookings retain the locked amount.
3. Configure draft supplier identity without GSTIN. Save successfully; verify invoice issuance remains blocked but the customer's ordinary payment receipt PDF downloads.
4. Save real merchant tax details and approved tax policies (staging values only). Create paid booking, membership, training and tournament purchases. Verify invoice tax, discounts and loyalty tender match the saved payment snapshot; test missing customer billing information.
5. Request and download invoice/credit note through customer account; try another user's payment ID or document number and verify access denied.
6. Refund partially then fully; reconcile cash refund, tax credit note and loyalty points without duplicate activity. An already-fully-refunded purchase must **not** receive a newly issued tax invoice.
7. Confirm email delivery disabled: create invoice, inspect delivery queue and SMTP provider for **zero sends**. No email should be sent just because a booking completes.
8. Operator verifies identity, collects onsite amount and performs check-in/out; customer immediately sees correct booking and payment status.
9. Check screen widths 320px/375px/768px/desktop; labels, focus indicators, tab order, logical empty states, readable names instead of internal code or ID and mobile-native navigation.
10. Export/print receipt and issued invoice with long addresses, multilingual customer names, credits and all required tax fields. Correct invalid PDF rendering or missing glyphs before production.

## Deferrals for the next release

- New business features requested after this scope freeze.
- Opaque individualized/dynamic price discrimination, device-based surcharges and unapproved behavioral pricing.
- Broader advanced loyalty segmentation beyond already implemented rules.
- Additional notification channels or automated marketing requirements not already delivered.
- IRP integration when required but provider credentials/approval are not yet available; remains a controlled compliance prerequisite, not a hidden claim of completion.

**Release decision:** A successful CI run is necessary, but production approval requires the staging/UAT and legal gates above. Record actual execution evidence and defects; leave unexecuted tests as **NOT VERIFIED**, not passed.
