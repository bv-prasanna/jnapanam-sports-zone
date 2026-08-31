# Module 07 — Notifications

## Included

- In-app customer notification inbox and read acknowledgement
- Email, SMS, push and in-app channel model
- Per-customer email/SMS/push preferences
- Configurable notification templates
- Durable notification outbox with status and attempt tracking
- Admin broadcasts and controlled outbox dispatch
- Automatic payment-success and refund-processed notifications
- Regression coverage for notification preferences, broadcasts and payment events

## APIs

- `GET /api/v1/notifications/me`
- `PATCH /api/v1/notifications/{id}/read`
- `GET /api/v1/notifications/preferences`
- `PUT /api/v1/notifications/preferences`
- `GET /api/v1/admin/notifications/templates`
- `POST /api/v1/admin/notifications/templates`
- `PUT /api/v1/admin/notifications/templates/{id}`
- `POST /api/v1/admin/notifications/broadcasts`
- `POST /api/v1/admin/notifications/dispatch`

External email/SMS/push providers are intentionally represented by the durable outbox. Provider credentials and adapters can be connected at deployment without changing customer-facing APIs.
