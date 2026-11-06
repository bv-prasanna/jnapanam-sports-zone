# Refund adjustments and no-show exceptions (UAT)

An administrator may refund a captured Razorpay booking after its status is CANCELLED or NO_SHOW. For a past NO_SHOW, a specific genuine exception reason (at least 10 characters) is mandatory. Normal cancellation remains separate from a refund.

## Calculation and audit

Refundable balance = captured payment − sum of **processed** refunds.
Admin enters retained charge, and the refundable amount is calculated as balance − retained charge.
The server validates the same equation in paise. A Razorpay refund must be at least ₹1.

Example: ₹150 captured, ₹20 retained, ₹130 refund submitted. The retained charge is recorded with the refund request and remains with the merchant; no extra collection occurs. Refund reason and timestamp are persisted, and Admin Payments shows refund history.

## Reconciliation and safety

- Only ADMIN/SUPER_ADMIN may submit a refund through this endpoint.
- Require the booking to be CANCELLED or NO_SHOW. Active bookings must be cancelled first.
- Only RAZORPAY/MOCK captured payments are eligible; ONSITE/CASH payment requires separate manual refund management.
- Reject excess refunds, invalid deductions and concurrent REQUESTED refunds.
- The Razorpay request uses X-Refund-Idempotency based on the saved refund UUID.
- If the gateway response is uncertain, the request remains REQUESTED to prevent an unsafe duplicate; reconcile in Razorpay before proceeding.
- Razorpay refunds are REQUESTED, not marked complete until a verified refund.processed webhook. Show a pending notification, not a completed-refund message.
- Audit records contain amount, retained charge, reason and timestamps. No card details, OTP, API secrets or webhook bodies are stored with these refund records.

### Deployment checks

Apply Flyway V60, deploy backend and web, and use a captured Razorpay test transaction before live UAT. Confirm Razorpay Dashboard refund balance and test/live mode. Verify a submitted refund and a processed webhook, partial refund history, no-show exception and duplicate submission. Avoid automatic deductions without a published cancellation/fee policy and corresponding tax/invoice treatment.
