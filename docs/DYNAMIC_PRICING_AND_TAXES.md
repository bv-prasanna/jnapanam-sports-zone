# Dynamic pricing and tax rules — Jnapanam

Status: **Phase 1 calculation engine only**. Not yet integrated with persistent admin UI, slot quoting, booking snapshots, checkout, or invoices.

## Scopes and precedence
- Supported scopes: SITE (whole arena), SPORT, FACILITY, SLOT. Most specific matching rule wins: SLOT > FACILITY > SPORT > SITE.
- Among matching rules at the same scope, higher priority wins; use stable rule IDs to resolve ties. Price rules are **not additive across scopes** by default; the selected rule changes the facility base price.
- Adjustment kinds: fixed price, amount increase/decrease, percentage increase/decrease.
- Match local date, start time, weekday and effective date window. Time intervals are start-inclusive/end-exclusive. Split overnight periods into separate rules.

## Tax
- Configure tax rule at SITE, SPORT, FACILITY or SLOT scope, and separately specify whether published prices are tax-inclusive or tax-exclusive.
- Most specific matching tax rule wins; tax rules do not stack by scope. Actual applicability, GST rate, exemptions and invoice treatment require legal/tax review and must not be inferred from this example calculator.
- Taxes apply after dynamic price adjustments and before payment. Return net, tax and total rounded to paise.

## Required backend work before use
1. Add versioned PostgreSQL pricing-rule and tax-rule tables with tenant/site identifiers, scope references, status, effective windows, timezone and auditing.
2. Add authorized admin APIs for create/update/disable, preview simulations and conflict detection. Prohibit retroactive alteration of captured booking prices.
3. Add a quote endpoint (sport, facility, slot, date, quantity) used by **web and mobile**. Lock/snapshot quote, applied rule version, tax breakdown and final amount in booking/payment records.
4. Ensure every payment path (Razorpay, cash, operator collection, refunds and reschedules) charges/refunds the **stored booking amount**, not the current facility price. Handle coupon stacking and membership benefits explicitly.
5. Show customer-visible final price, savings, taxes and final payable at slot selection and checkout. Display price breakdown on receipt/GST invoice.
6. Admin dashboard: bulk peak/off-peak pricing across days/hours; holiday pricing; site-wide overrides; per-sport/facility/slot; preview calendar, activation dates and audit history.
7. Test precedence, weekday/timezones, date boundaries, tax-inclusive/exclusive cases, discounts, amounts, concurrency, quote expiry, cancellation/refund, idempotent checkout and backwards compatibility.
8. Validate tax settings with the accountant before enabling live calculations. Do not hard-code a universal GST rate.

## Illustrative example (not a statement of tax law)
Base fee ₹500 per court-hour; peak-hour surcharge 20% = ₹600; 18% *configured illustrative* tax-exclusive rate = ₹108; payable ₹708.
Off-peak fixed discount ₹100 = ₹400 before tax. Existing booked customers retain their captured total when pricing changes.
