# Admin Taxes, Offers, Coupons, Loyalty and Engagement Pricing

Status: product and backend contract specification; **not yet implemented as working screens or payment-integrated APIs**. Existing promotions and loyalty modules must be reused rather than duplicated.

## Admin navigation
- **Taxes**: list/search/create/edit/disable tax rules, with scope picker Entire site / Sport / Facility / Individual slot. Sports/facilities/slots are *lookup selectors*, not code inputs. Support tax name, tax components (e.g. CGST/SGST or IGST when applicable), configured rates, inclusive/exclusive, activation windows, priority, status, and audit. Never hard-delete a tax rule referenced by an invoice; disable/version it.
- **Dynamic Pricing**: same scope selector, base price/percentage/flat adjustment, weekdays, local start/end times, effective dates, peak/off-peak label, conflict preview, bulk court selection, calendar preview and audit.
- **Offers & Coupons**: extend current Promotion entities/API, display customer-facing coupon code only when a coupon is deliberately shared. Automatic offers never require a code. Include purposes, eligibility, minimum spend, start/end dates, usage limits, stackability, coupon application and redemption audit.
- **Loyalty**: existing wallet and transaction service, visible balance, earning/redemption settings, configurable tiers, expiry/return rules and earned/redeemed history. Prevent double redemption, support reversal on refund.
- **Customer Segments**: rule builder for disclosed, objective engagement criteria (completed bookings, visits in the last N days, membership, loyalty tier, new/returning customer). Applies optional capped discounts and benefits; explain the benefit on the price breakdown. No individual opaque surcharge, device OS/iPhone/Android-based price penalty, or use of protected or sensitive traits.

## Business logic
1. Resolve booking base facility fee and active site/sport/facility/slot pricing rule. Most specific wins unless a separately configured explicit stack policy exists.
2. Resolve segment eligibility from verified server-side engagement history. Default is the same advertised price for everyone, with clearly labeled extra rewards/discounts for qualified members.
3. Apply eligible offer or coupon, then allowable loyalty points redemption according to a defined stacking policy. Caps and minimum final price are server-enforced; coupon use is atomic.
4. Resolve applicable tax rule *after* legally appropriate discounts; exclusive and inclusive calculations must be supported. Tax components and invoice rules require accountant approval; do not hardcode a universal rate.
5. Provide quote preview to web/mobile; at booking confirmation store immutable versioned breakdown (base fee, surge/off-peak adjustment, offer, coupon, loyalty, taxable value, tax components, total). Razorpay and operator payments use the captured total, never recompute current rates.
6. Refund/cancellation/reschedule must use original snapshot and preserve historical invoice values. Effective-dated rule edits must not rewrite previously confirmed bookings.
7. Backend enforcement: RBAC for tax/pricing edits, audit actor and timestamps, ownership of references, idempotent code/redemption operations, server-side validation, per-rule overlap diagnostics and deterministic precedence.

## API proposal (not live)
- GET/POST /api/v1/admin/taxes
- PUT /api/v1/admin/taxes/{id}; PATCH /api/v1/admin/taxes/{id}/status
- GET/POST /api/v1/admin/pricing-rules
- PUT /api/v1/admin/pricing-rules/{id}; PATCH /api/v1/admin/pricing-rules/{id}/status
- POST /api/v1/admin/pricing-rules/preview
- GET/POST /api/v1/admin/engagement-segments
- POST /api/v1/bookings/quotes
- Reuse /api/v1/admin/promotions, existing loyalty APIs and payment checkout endpoints, avoiding duplicate ledgers.

## Regression / UAT acceptance
- All four tax scopes; edit and disable; no destructive deletion for used rules; tax-inclusive/exclusive; tax component rounding.
- All four pricing scopes; weekday/peak/offpeak/holiday; midnight timezone; effective date boundaries; precedence and conflict resolution.
- Identical web/mobile quote for same request; quote expiry; bookings retain old prices after rule changes.
- Promotion eligibility, single-use/multi-use limits, coupon stacking and race conditions.
- Loyalty earn/redeem/reversal, duplicate webhook replay, partial refund allocation and ledger reconciliation.
- Segment qualifies based on booking history; benefits disclosed, capped, and consistent across platforms. No OS-based, inferred wealth-based or undisclosed individual price escalation.
- Role checks, audit logs, negative API tests, responsive admin pages and keyboard/screen-reader accessibility.
