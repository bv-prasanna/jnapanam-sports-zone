# Jnapanam — Security, status codes, logging and release findings

**Reviewed:** 2026-11-10; source and GitHub CI only. Not a penetration test, live deployment inspection, or provider/DB audit.

| Priority | Source finding | Action / evidence needed |
|---|---|---|
| High — addressed in this PR | AuthRateLimitFilter used caller-supplied X-Forwarded-For, permitting IP spoofing to change rate-limit key | Use request.getRemoteAddr, 429 AUTH_RATE_LIMITED with Retry-After/correlation ID; regression asserts spoofing does not bypass limiter |
| High — addressed in this PR | Android login compared English error message to guess MFA_REQUIRED | Use backend ProblemDetail.code, show separate wrong password/MFA invalid/SMTP outage, clear stale codes, allow resend with Kannada labels |
| High — release blocker | Default configuration includes a non-production JWT_SECRET, 30-day access-token fallback and mock payment provider | Require a distinct production secret, shorter approved JWT_ACCESS_MINUTES, live Razorpay mode with verified webhooks and safe rollback; **do not deploy sample defaults** |
| High — infrastructure blocker | Per-instance auth limiter is not distributed; remoteAddr may be a shared Northflank ingress | Configure trusted reverse-proxy handling and edge/WAF limits; verify false positives and distributed attacks before go-live |
| High — external UAT pending | Razorpay refunds previously rejected; merchant refund permissions and balances not verified | Confirm captured payment and refundable balance, signed gateway events, refund IDs and ledger reconciliation |
| High — external UAT pending | MFA email failed to arrive in earlier UAT | Test SMTP in Admin Notification Providers and Northflank; provider acceptance is not inbox delivery |
| Medium — security review pending | Web refresh tokens live in localStorage; customer document upload/administrative screens handle private data | CSP and XSS regression, strict backend RBAC, media access control, retention and incident response review |
| Medium — UX review pending | Several web/mobile screens use static English text or rely on shared translation | Native Kannada QA and full route-by-route visual/device testing |
| Medium — ops gate pending | Existing docs are source-derived; production DB schema, backup restore, environment regions and signing are not proven | Record deployed SHA, introspection result, Flyway checksums, restored clone, SLO monitors and incident contacts |

## API and admin log contract

- 400 validation, 401 invalid credentials/MFA_REQUIRED/MFA_INVALID, 403 forbidden, 404 missing/owner-safe, 409 conflict, 422 business rule, 426 mandatory update, 429 AUTH_RATE_LIMITED, 503 MFA_DELIVERY_FAILED / REFUND_STATUS_UNCERTAIN.
- Use the structured code rather than matching English error messages. Include X-Correlation-ID and requestId where available; pass Retry-After to throttled clients.
- Logs should record sanitized route, method, status, duration, role/actor (where appropriate), safe error class and correlation ID. **Never log raw credentials, JWT/refresh tokens, OTP, provider secrets, PAN, identity images, payment card data or customer body payloads.**
- Payment “created”, “captured”, “refunding” and “refunded” are distinct states; no premature success toast.

## Recommended release order

1. Review and merge only compatible PRs; record a single release SHA and green backend, web and mobile CI.
2. Freeze application version and inspect [deployment](DEPLOYMENT_TOPOLOGY.md), [architecture](SYSTEM_ARCHITECTURE.md), [ER](CORE_ER.md), [schema and safe migrations](DB_SCHEMA_AND_MIGRATION.md), and [KT demo](KT_DEMO.md).
3. In verified isolated staging, take an encrypted database backup, restore-test, validate Flyway V1–V61 history and test Razorpay/SMTP using sandbox accounts.
4. Execute [every-route test register](PAGE_BY_PAGE_UAT_2026-11-10.md); resolve all P0/P1 payment/booking/auth defects.
5. Deploy backend then web and a **signed** Android build; verify CDN CORS/HTTPS, health, rate limit and incident/rollback procedures.
6. Record UAT evidence, role owners, sign-offs and monitoring on the release issue.

**Certification: pending external UAT/ops evidence.** No new DDL migration is required for this review; the existing guarded migration script [migrate-safe.sh](migrate-safe.sh) and read-only source checker [verify-flyway-source.sh](../../scripts/verify-flyway-source.sh) remain authoritative.
