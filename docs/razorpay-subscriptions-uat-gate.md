# Jnapanam — recurring billing UAT and release gate

## Architecture
- One-time checkout for booking, membership, training and tournament remains supported.
- Recurring billing is **opt-in**, with `RECURRING_SUBSCRIPTIONS_ENABLED=false` by default.
- Use Razorpay **Subscriptions** plans (not the ordinary Payment Orders API) for recurring memberships or training installments.
- Admin can configure weekly, monthly or yearly offerings, amount per cycle, interval and finite cycle count.
- Training offerings are bounded by the active batch's end date. Membership plan duration must approximately match its recurring cadence.
- Customer consent is explicit. Razorpay mandate authorization is not equivalent to a successful captured installment.
- Verified `subscription.charged` webhooks create one record in `recurring_charges` per Razorpay payment ID, and only then activate/extend an eligible membership or mark the training installment paid.
- Other subscription lifecycle events change mandate state but never grant paid entitlements.
- Customer/admin cancellation calls Razorpay cancel immediately. Previously paid benefits remain until their ordinary end date.
- Failed/cancelled/expired authorizations cannot be treated as successful payment.

## Setup prerequisites
1. Northflank: `PAYMENT_PROVIDER=razorpay`, `RAZORPAY_KEY_ID=rzp_test_...`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET` in server-side secrets only.
2. Razorpay account must actually have **Subscriptions** enabled in Test Mode. The ability to use test keys does **not** imply Subscriptions activation.
3. Under Razorpay Test Mode webhooks, enable at minimum:
   `subscription.authenticated`, `subscription.activated`, `subscription.charged`,
   `subscription.pending`, `subscription.halted`, `subscription.cancelled`, `subscription.completed`,
   `subscription.paused`, `subscription.resumed`, `subscription.updated`.
   Subscription events may only appear after the product is enabled for this account.
4. Webhook endpoint is the existing `POST /api/v1/public/payments/webhooks/razorpay`; retain the same secret for all selected event categories.
5. Deploy and verify Flyway V46/V47, and ensure JPA startup validates. **Do not activate recurring charges merely because migration succeeded.**
6. In Admin → Recurring Subscriptions, create test plans, check pricing, duration, capacity constraints and whether Razorpay accepted them.
7. Publish one test offering, then set `RECURRING_SUBSCRIPTIONS_ENABLED=true` **only in the isolated Test Mode backend** after the above checks.

## Required UAT before a release APK
- Customer authenticates mandate (success, denial, retry, expired link) and sees no paid access until the first captured installment.
- Test charges verify signature, provider plan ID, payment ID, exact amount, INR currency, and `captured` status.
- Duplicate webhook, invalid signature, altered amount/currency, replay and out-of-order events cannot double-activate or extend benefits.
- Member access extends for each actual captured billing period; no duplicate renewal, and expired membership resumes only when legitimate charges occur.
- Training installments stop by batch end; cannot charge suspended or cancelled training or exceed configured cycle count.
- Customer cancels from web and Android; Super Admin cancels; confirm in Razorpay portal no future charge.
- Check recurring customer and admin charge history, duplicate payments and refunds.
- Test `payment.failed`, `refund.created`, `refund.processed`, `refund.failed`, `order.paid`, `payment.authorized`, settlement reconciliation.
- Confirm SMS/email/IN_APP alerts for important events with configured provider credentials.
- Confirm normal one-time payment remains functional, with no double-payment path for a recurring enrollment.
- Confirm payment failures do not silently cancel otherwise valid bookings or training; reconcile unpaid or anomalous provider charges manually.

## Limitations / launch blockers
- This implementation currently relies on Razorpay Test Mode feature enablement and externally triggered webhooks. CI cannot exercise the real mandate flow without a connected Razorpay account with subscriptions enabled.
- A recurring charge ledger is provided; a complete GST tax invoice per recurring charge and automatic bank settlement reconciliation are **not** implemented. These need explicit finance acceptance before enabling **Live** recurring billing.
- If a provider charge occurs for an invalid/expired enrollment, webhook processing fails and requires manual review/refund; do not quietly grant access.
- Scheduled background jobs depend on the Spring backend remaining online.
- Approval of payment-method availability (UPI AutoPay/card/eMandate) and RBI mandate limits is account/provider dependent; verify in Razorpay dashboard.
- The release Android APK should only be rebuilt from a merged, green, reviewed commit after E2E UAT, not this draft branch.
