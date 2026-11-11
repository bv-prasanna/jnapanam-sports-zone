# Razorpay Live Mode: Jnapanam release checklist

**Do not switch to live payment collection merely because KYC was submitted.** Confirm Razorpay Live Mode is activated and jnapanam.in website is approved in the account.

## Security prerequisite — Northflank security incident, 30 October 2026
Assume every previously stored secret in Northflank may be exposed. Revoke and rotate the database password, JWT secret, API keys, email/SMS credentials, old Razorpay keys and webhook secret. Review audit logs, disable public access to database add-ons. Avoid adding newly issued live payment credentials until the service/environment is secured.

## Secure backend environment settings on Northflank
Configure these in service **Environment** (never paste secret values into tickets, chat, GitHub, or APK):
- PAYMENT_PROVIDER=razorpay
- RAZORPAY_KEY_ID=<fresh Razorpay Live Mode key ID>
- RAZORPAY_KEY_SECRET=<fresh Razorpay Live Mode API secret>
- RAZORPAY_WEBHOOK_SECRET=<distinct new live webhook signing secret>
- RECURRING_SUBSCRIPTIONS_ENABLED=false

Ensure DB_* and JWT_SECRET are updated to freshly rotated credentials. Deploy backend.

## Razorpay dashboard Live Mode
- Configure auto-capture (Orders API is already used by Jnapanam).
- Register site jnapanam.in and webhook URL:
  https://p01--jnapanam-sports-zone--v9mpkwy69gfr.code.run/api/v1/public/payments/webhooks/razorpay
- Webhook events: payment.captured, payment.failed, refund.processed, refund.failed.
- Webhook secret equals RAZORPAY_WEBHOOK_SECRET (not the API key secret).
- Do not enable recurring subscriptions until separate mandate UAT is complete.

## Real-money UAT (small legitimate transaction)
1. Verify current backend/web deployments, pricing quotes, T&C and available slots.
2. Book low-value real service and pay via Razorpay Live Mode.
3. Confirm same order ID, currency INR, amount, and status **captured** on Razorpay dashboard.
4. Confirm app and backend show PAID, booking fulfilled once, notifications/tax invoice generated; no duplicate webhook fulfilment.
5. Test authorized-but-not-captured path: payment remains pending, no booking fulfilment.
6. Test refund, webhook delivery status and settlement reconciliation before wider rollout.

The public Checkout uses only the key ID. Never bundle RAZORPAY_KEY_SECRET or the webhook signing secret in Android.
