# UAT to Production Release Gate — Jnapanam Sports Zone
This checklist documents deployment configuration observed on Northflank on 2026-10-21. These are **not automatically reverted**; operators must validate each item before real customer use.

## Current validated state
- Northflank backend (Java 21), PostgreSQL 17. Local `/actuator/health` returned HTTP 200 after setting `MANAGEMENT_HEALTH_MAIL_ENABLED=false`.
- Public health endpoint and mobile customer workflows still require end-to-end verification.
- SMTP is not configured; live SMS OTP and payment acceptance must be independently validated.

## UAT-only changes and required production treatment
| Setting | UAT | Production gate |
|---|---|---|
| `MANAGEMENT_HEALTH_MAIL_ENABLED` | false | Enable only after real SMTP provider works; or document deliberate exclusion with separate alerting |
| `MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS` | always for troubleshooting | Remove override or use `when_authorized` / `never` |
| `MANAGEMENT_ENDPOINT_HEALTH_SHOW_COMPONENTS` | always for troubleshooting | Remove override or use `when_authorized` / `never` |
| `app.security.test-otp-enabled` | May be enabled in private UAT ONLY | false, enforce application prod-profile guard |
| Fixed test OTP 1977 | Private closed UAT only | REMOVE; never publish fixed OTP on a public auth API |
| `SPRING_PROFILES_ACTIVE` | prod observed in Northflank | For fixed OTP use separate isolated UAT environment/profile with access restrictions |
| `PAYMENT_PROVIDER` | mock/test | Real provider configured; webhooks signed and verified; reconciliations tested |
| `SPRING_DATASOURCE_URL` and `DB_URL` | Credential-free JDBC URL | Verify effective values; avoid conflicts and duplicate precedence |
| `DB_USERNAME`, `DB_PASSWORD` | Northflank secret group | Rotate exposed credentials; runtime-secret only; restore/backup checked |
| `JWT_SECRET` | Northflank secret group | Rotate exposed key; document that rotation revokes existing tokens |
| `CORS_ALLOWED_ORIGINS` | Cloudflare workers.dev origin | Restrict to approved web origins |
| `SPRING_FLYWAY_ENABLED` | true | Keep with pre-migration backup and staging restore rehearsal |
| `SMTP_*`, `FAST2SMS_*` | Not configured for live delivery | Set real credentials, verify provider failures/retries and registration/reset delivery |
| Health check | HTTP /actuator/health, port 8080, extended startup delay | Keep readiness but evaluate dedicated liveness/readiness paths; never bypass failed DB health to green-light launch |

## Release sequence
1. Verify public HTTPS `/actuator/health` returns 200; ensure details are not exposed publicly.
2. Run Android cloud build on an approved release SHA with a functioning HTTPS backend origin. GitHub workflow creates a **debug APK**, not a production release-signed artifact.
3. Install APK on a physical device; test customer login, book/cancel, operator QR/check-in, shopkeeper counter and permissions.
4. Run tests, review GitHub PRs, secure credentials, verify backup/restore and deployment rollback.
5. Only then switch on real OTP and payment providers, disable any UAT-only override, and create a signed production APK/AAB with controlled signing keys.
