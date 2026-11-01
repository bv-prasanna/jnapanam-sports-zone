# Dynamic pricing and tax rules — Jnapanam

Status: **Implemented in draft PR #64**: calculation engine, Flyway migrations V48–V50, admin price/tax management, public quotes, locked booking price/tax, checkout settlement and manually reviewed billing documents. **Not production released**; GST configuration, discounted-order tax allocation, Razorpay sandbox verification, PDF/IRN delivery and live UAT remain required.

## Scopes and precedence
- Supported scopes: SITE (whole arena), SPORT, FACILITY, SLOT. Most specific matching rule wins: SLOT > FACILITY > SPORT > SITE.
- Among matching rules at the same scope, higher priority wins; use stable rule IDs to resolve ties. Price rules are **not additive across scopes** by default; the selected rule changes the facility base price.
- Adjustment kinds: fixed price, amount increase/decrease, percentage increase/decrease.
- Match local date, start time, weekday and effective date window. Time intervals are start-inclusive/end-exclusive. Split overnight periods into separate rules.

## Tax
- Configure tax rule at SITE, SPORT, FACILITY or SLOT scope, and separately specify whether published prices are tax-inclusive or tax-exclusive.
- Most specific matching tax rule wins; tax rules do not stack by scope. Actual applicability, GST rate, exemptions and invoice treatment require legal/tax review and must not be inferred from this example calculator.
- Taxes apply after dynamic price adjustments and before payment. Return net, tax and total rounded to paise.

## Operational release checks and remaining enhancements
1. Verify Flyway V48–V50 on a backup/restorable staging database. Site-scoped rules exist; tenant-specific identity, detailed change audit and pricing rule versions remain future hardening.
2. Verify admin create/edit/deactivate and tax overlap rejection; price preview is available for individual slots but bulk simulation is not yet implemented.
3. Web and mobile use public slot price quotes. Confirm rejects mismatched accepted totals; the booking locks a money/tax snapshot. Add automated end-to-end concurrency tests before launch.
4. Booking payment creation and operator collection use stored booking totals; refunds use captured payment-order amounts. Price-changing reschedules are rejected pending a reviewed adjustment workflow. Discount/loyalty GST allocation remains gated for manual review.
5. Web/mobile display final slot quote and tax; online discounts are applied later by the existing payment engine. Validate wallet/coupon checkout breakdown and offer eligibility on real devices.
6. Admin pricing/tax rule screens exist with dates/weekdays/times and scoped selection; bulk calendar editor and full edit audit remain pending.
7. CI covers calculator, billing guards and booking snapshot invariants; verify database CRUD, tenant security, discount tax treatment, refund webhook replay, active quote boundaries and GST bill rendering in staging.
8. Populate actual supplier legal name, GSTIN, address, state and SAC; have an accountant verify place-of-supply, CGST/SGST vs IGST, discount/loyalty allocations, e-invoicing applicability and credit-note requirements. Do not hard-code a universal GST rate.

## Illustrative example (not a statement of tax law)
Base fee ₹500 per court-hour; peak-hour surcharge 20% = ₹600; 18% *configured illustrative* tax-exclusive rate = ₹108; payable ₹708.
Off-peak fixed discount ₹100 = ₹400 before tax. Existing booked customers retain their captured total when pricing changes.
