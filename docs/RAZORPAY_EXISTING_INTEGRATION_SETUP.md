# Razorpay setup in Jnapanam (no duplicate integration)

The Spring Boot backend already has:
- POST /api/v1/payments/orders (server-side Orders API)
- Web Standard Checkout (loads checkout.js and receives id/signature)
- POST /api/v1/payments/verify (HMAC signature + authenticated capture/order/amount/currency check)
- Signed payment/refund webhooks
- Admin payment and reconciliation views

## Credential environments
The standard Razorpay integration prompt can display **test API keys**.
Do NOT paste any key or secret into AI chats, GitHub, Android/Cloudflare builds or frontend environment variables.

A test key ID starts with rzp_test_; it cannot collect actual money.
For production, use fresh approved Razorpay **Live Mode** keys starting with rzp_live_.
If a secret was pasted into chat or otherwise exposed, regenerate/rotate the key pair.

Store the pair only in Northflank service **Environment**:
- PAYMENT_PROVIDER=razorpay
- RAZORPAY_KEY_ID=<key from selected mode>
- RAZORPAY_KEY_SECRET=<corresponding secret from same key pair>
- RAZORPAY_WEBHOOK_SECRET=<separate secret set in Razorpay webhook>
- RECURRING_SUBSCRIPTIONS_ENABLED=false

**Do not substitute test credentials into the live backend.** Use a separate test backend deployment
and database for Test Mode booking/payment UAT, preferably with a dedicated test hostname.

## Diagnosis: Razorpay rejected an order
The backend now emits a safe log with the Razorpay HTTP status, error code, field, and
short alphanumeric reason. No secret, raw response, payment PAN, bank data or header is logged.
- 401 = inspect live/test key pair, rotation, account mismatch
- 403 = inspect merchant account/payment permissions
- 400/422 = inspect order amount, currency, receipt, eligibility; contact support with booking id
- 429/5xx = provider busy or unavailable; do not automatically retry payment creation

Find logs in Northflank service > Logs, search `Razorpay create_order rejected`.
Inspect payment orders in Jnapanam Admin > Payments and payment status in Razorpay Dashboard
before retrying; booking slots can remain temporarily reserved for pending payments.
Only captured money is counted as paid.

## Live switch prerequisites
Confirm KYC plus Live Mode activation, website verification, permitted methods, webhook delivery
and automatic payment capture. Rotate affected Northflank credentials following the 2026 incident
before storing newly issued live keys. Run a low-value genuine payment and refund UAT and
reconcile amount/order/status before full customer launch.

## Standard Checkout prompt: implementation mapping
- Backend: `PaymentController` exposes `POST /api/v1/payments/orders`. PaymentService owns the
  amount, receipt and idempotency; RazorpayGateway calls `POST /v1/orders`.
  A minimum 100 paise is enforced after discounts. The UI never sets the amount.
- Web: `openPaymentCheckout` in `apps/web/app/dashboard/page.tsx` loads checkout.js,
  uses the returned gateway order ID, validates all callback fields and captures
  `payment.failed` / modal dismiss without treating the failed attempt as paid.
- Verification: `POST /api/v1/payments/verify` validates HMAC-SHA256 using the order
  ID stored on the server, not a browser-trusted order; it additionally fetches the
  payment from Razorpay to verify captured status, order, currency and amount.
- Mobile: checkout is hosted server-side using a short-lived session, then receipt
  status is checked; closing the browser is never considered successful payment.
- There is **no need** to install a Node SDK for this Java / Spring Boot backend.
- **APK builds are manual only** in `.github/workflows/android-apk-cloud.yml`.
