# Module 20 — Merchandise Commerce

## Included
- bilingual category and product catalogue
- product variants, prices, tax and stock thresholds
- persistent customer cart
- atomic checkout with row-level stock locking
- facility pickup and delivery
- immutable order-item price/name snapshots
- customer order history and cancellation with stock restoration
- admin catalogue creation, stock adjustment and order status management

## APIs
Public catalogue: `GET /api/v1/commerce/categories`, `GET /api/v1/commerce/products`

Authenticated customer: cart, checkout, orders and cancellation under `/api/v1/commerce`.

Staff: catalogue, inventory and fulfilment under `/api/v1/admin/commerce`.

Online gateway settlement is intentionally separated from order creation. `PAY_AT_PICKUP` is usable immediately; online payment fulfilment will be connected through the existing payment provider in the next slice.
