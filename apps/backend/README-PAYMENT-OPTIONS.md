# Optional booking payment patch

This patch adds `PAY_NOW` and `PAY_AT_VENUE` to paid facility bookings while preserving the existing online-payment flow.

## Apply on Windows

Run from PowerShell after downloading the ZIP:

```powershell
$zipPath = "$HOME\Downloads\Jnapanam-Backend-Patch-Optional-Booking-Payment-v1.zip"
$updatePath = "C:\Projects\jnapanam-payment-options-update"
$backendPath = "C:\Projects\jnapanam-sports-zone\apps\backend"

Remove-Item $updatePath -Recurse -Force -ErrorAction SilentlyContinue
Expand-Archive $zipPath -DestinationPath $updatePath -Force
Copy-Item "$updatePath\backend-payment-options-patch\*" $backendPath -Recurse -Force

Set-Location $backendPath
mvn clean verify
```

Expected result: `BUILD SUCCESS` and one additional integration test.

Start the backend:

```powershell
docker compose up -d
mvn spring-boot:run
```

Flyway applies `V15__booking_payment_options.sql` automatically.

## API behavior

### Customer chooses payment at venue

`POST /api/v1/bookings/confirm`

```json
{
  "holdId": "<hold-id>",
  "paymentOption": "PAY_AT_VENUE"
}
```

The booking is immediately `CONFIRMED` with `paymentStatus: UNPAID`. It cannot be checked in until staff collect the payment.

### Customer chooses online payment

```json
{
  "holdId": "<hold-id>",
  "paymentOption": "PAY_NOW"
}
```

The booking remains `PENDING_PAYMENT` and uses the existing payment-order and verification APIs. If `paymentOption` is omitted, it defaults to `PAY_NOW` for backward compatibility.

### Staff collect payment at venue

`POST /api/v1/operations/bookings/{bookingId}/collect-payment`

```json
{
  "paymentMethod": "UPI",
  "reference": "upi-reference-123"
}
```

Supported methods: `CASH`, `UPI`, and `CARD`. The endpoint records an `ONSITE` paid payment order for revenue reporting and marks the booking paid.

### Admin requires a customer to pay upfront

`PATCH /api/v1/admin/users/{userId}/payment-policy`

```json
{
  "requiresUpfrontPayment": true
}
```

That customer can no longer confirm a paid booking with `PAY_AT_VENUE`. Set the value to `false` to restore both choices.

## Acceptance checks

1. Normal customer + `PAY_AT_VENUE` returns `CONFIRMED` and `UNPAID`.
2. Check-in before venue payment returns `409`.
3. Collect venue payment returns provider `ONSITE` and status `PAID`.
4. Check-in after payment succeeds.
5. Customer with `requiresUpfrontPayment: true` receives `409` for `PAY_AT_VENUE`.
6. `PAY_NOW` continues through the existing online-payment flow.

Coupons, promotional offers, online-payment discounts, and loyalty points are intentionally not included in this focused patch; they should be delivered as the next pricing and rewards module.
