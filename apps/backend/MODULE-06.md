# Module 06 — Payments and Refunds

## Included

- Server-calculated prices for bookings, memberships and training enrollments
- Razorpay order creation using Java's HTTP client
- Checkout signature verification using HMAC-SHA256
- Idempotent payment-order creation through `Idempotency-Key`
- Razorpay webhook signature verification and event deduplication
- Customer payment history and receipt API
- Admin payment list and full/partial refunds
- Automatic membership activation after successful payment
- Mock payment gateway for local development and automated tests

## Local development

The default provider is `mock`; no Razorpay account is needed. Verify a mock payment with
`signature: "mock-signature"`.

For Razorpay Test Mode, set:

```text
PAYMENT_PROVIDER=razorpay
RAZORPAY_KEY_ID=rzp_test_...
RAZORPAY_KEY_SECRET=...
RAZORPAY_WEBHOOK_SECRET=...
```

Configure the Razorpay webhook URL as:

```text
https://YOUR_PUBLIC_BACKEND/api/v1/public/payments/webhooks/razorpay
```

Subscribe at minimum to `payment.captured`. Never commit real keys.

## Main APIs

- `POST /api/v1/payments/orders` (`Idempotency-Key` header required)
- `POST /api/v1/payments/verify`
- `GET /api/v1/payments/me`
- `GET /api/v1/payments/{id}/receipt`
- `GET /api/v1/admin/payments`
- `POST /api/v1/admin/payments/{id}/refunds`
