# Module 15 — Promotions, offers and loyalty

Prerequisite: the optional booking payment patch (`V15`) must already be installed.

## Install on Windows

```powershell
$zipPath = "$HOME\Downloads\Jnapanam-Backend-Module-15-Promotions-Loyalty.zip"
$updatePath = "C:\Projects\jnapanam-module15-update"
$backendPath = "C:\Projects\jnapanam-sports-zone\apps\backend"

Remove-Item $updatePath -Recurse -Force -ErrorAction SilentlyContinue
Expand-Archive $zipPath -DestinationPath $updatePath -Force
Copy-Item "$updatePath\backend-module-15\*" $backendPath -Recurse -Force

Set-Location $backendPath
mvn clean verify
```

Expected result: `BUILD SUCCESS` and **21 tests**.

Start the backend; Flyway applies `V16__promotions_and_loyalty.sql` automatically:

```powershell
docker compose up -d
mvn spring-boot:run
```

## Features

- Percentage and fixed-value coupons.
- Automatic online-payment offers when no coupon is supplied.
- Optional purpose restriction: booking, membership, training or tournament.
- Minimum order, maximum discount, validity dates, global limit and per-customer limit.
- Activate/deactivate by updating a promotion.
- Loyalty wallet, transaction history and Admin adjustments.
- Loyalty redemption during online checkout.
- Loyalty earning after verified online payment or venue payment.
- Full price breakdown on payment responses.

Existing payment requests remain valid. `couponCode` and `loyaltyPoints` are optional.

## Configuration

Defaults can be changed using environment variables:

```powershell
$env:LOYALTY_EARN_PER_100_RUPEES = "1"
$env:LOYALTY_POINT_VALUE_PAISE = "100"
$env:LOYALTY_MAX_REDEMPTION_PERCENT = "20"
```

The defaults mean: earn 1 point per ₹100 paid, each point is worth ₹1, and loyalty redemption may cover at most 20% of the order.

## Admin creates a coupon

`POST /api/v1/admin/promotions`

```json
{
  "code": "WELCOME10",
  "name": "Welcome discount",
  "description": "Ten percent off online membership payments",
  "discountType": "PERCENTAGE",
  "discountValue": 10,
  "maxDiscountPaise": 50000,
  "minOrderPaise": 50000,
  "purpose": "MEMBERSHIP",
  "automatic": false,
  "onlineOnly": true,
  "startsAt": "2026-09-01T00:00:00Z",
  "endsAt": "2026-12-31T23:59:59Z",
  "globalUsageLimit": 500,
  "perUserLimit": 1,
  "active": true
}
```

Use `automatic: true` to apply the offer automatically when the customer does not enter a coupon. Admin endpoints:

- `GET /api/v1/admin/promotions`
- `POST /api/v1/admin/promotions`
- `PUT /api/v1/admin/promotions/{id}`

## Customer checkout

`POST /api/v1/payments/orders` with a unique `Idempotency-Key` header:

```json
{
  "purpose": "MEMBERSHIP",
  "referenceId": "<membership-id>",
  "couponCode": "WELCOME10",
  "loyaltyPoints": 50
}
```

The response includes `originalAmountPaise`, `discountPaise`, `promotionCode`, `loyaltyPointsUsed`, final `amountPaise`, and `loyaltyPointsEarned` after payment verification.

## Loyalty APIs

Customer wallet and last 100 transactions:

`GET /api/v1/loyalty/me`

Admin views a wallet:

`GET /api/v1/admin/loyalty/{userId}`

Admin adds or removes points:

`POST /api/v1/admin/loyalty/{userId}/adjust`

```json
{
  "points": 100,
  "reason": "Launch welcome points"
}
```

Use a negative value to remove points. The balance cannot become negative.

## Important operational rule

Coupon usage and loyalty redemption are reserved when the payment order is created. In the first release, Admin should adjust the loyalty wallet if an abandoned payment requires manual reversal. Automated expiry/reversal can be added with the production payment-hardening work.
