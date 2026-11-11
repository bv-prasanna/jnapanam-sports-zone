# Jnapanam — app-skeleton reuse assessment and UAT/production handover

**Reference:** [varadatechworks/app-skeleton](https://github.com/varadatechworks/app-skeleton), inspected on 2026-11-10. This is an opt-in JavaScript/TypeScript module reference, **not** a drop-in replacement for Jnapanam's Spring Boot 4.1 / Java 21 backend, Vinext website or Expo app.

## Existing project versus reusable modules

| Common capability | Jnapanam source of truth | App-skeleton concept | Integration guidance |
|---|---|---|---|
| Authentication, MFA, device binding | `apps/backend/.../identity` and `security` | identity, authorization | Retain server-side Spring Security/JWT, review policies; do **not** replace with a client-only guard |
| Booking/operations state machines | `booking`, `admin` and `operations` | workflow, audit, geo | Reuse pure rule/test patterns only; preserve booking locks and existing Flyway constraints |
| Payments, tax, refunds | `payment`, `pricing`, `billing` | payments, billing | Retain Razorpay signature verification, idempotency, provider reconciliation and paise math |
| Notification providers | `notification` and provider settings | notifications | Compare consent/outbox/retry policy, never replace live transport without integration testing |
| Admin information architecture | `apps/web/app/admin` and shared navigation | admin, reporting | Adopt design tokens, empty/busy/error UI patterns, not entire Next admin templates |
| Feature visibility, translations | `apps/web/lib/features.ts`, `apps/mobile/src/i18n` | feature-flags, i18n | Map translation keys and server roles; prevent hiding alone from becoming authorization |
| Mobile release | `apps/mobile/src/lib/app-release.ts`, `app-management` | app-management | Preserve existing backend mandatory-version enforcement |

## Safe reuse rules

1. Keep the Java/PostgreSQL schema and versioned Flyway migration history authoritative. Do not apply app-skeleton PostgreSQL starter schemas to an existing database.
2. Reuse **pure, tested** business validators and visual patterns only after matching Jnapanam's existing enums, amounts in paise, role semantics and Kannada copy.
3. Do not introduce a second JWT provider, second payment ledger, second notification/outbox table or independent refund retry queue.
4. UI changes must share a design system: consistent typography, spacing, branded headers, tables with sticky heads and horizontal overflow, cards with comparable density, 44px minimum touch-target guidance, consistent pending/success/error tones, field errors and localized labels.
5. Prefer small PRs with screenshots + backend tests + web contract tests + physical-device evidence; never certify from source review alone.

## Operational scripts (repo-native)

```bash
# READ-ONLY migration source plan
bash scripts/verify-flyway-source.sh plan
# Environment-specific Flyway validation; requires verified protected FLYWAY_URL, FLYWAY_USER, FLYWAY_PASSWORD
bash scripts/verify-flyway-source.sh validate
# Backend quality gate (Java 21, Maven)
mvn -f apps/backend/pom.xml verify
# Web quality gate (Node >=22.13, from its working directory)
cd apps/web && npm ci && npm test && npm run lint
# Mobile quality gate, working directory apps/mobile
cd ../mobile && npm ci && npx tsc --noEmit && node --test tests/*.test.mjs
```

CI remains authoritative if its commands differ; compare the checked-in `.github/workflows/application-ci.yml` before certification.

## Required KT artefacts and acceptance

| Asset | Existing file | Release evidence needed |
|---|---|---|
| Deployment diagram | `DEPLOYMENT_TOPOLOGY.md` | Live Cloudflare/Northflank project IDs, URLs, Git SHAs, nonsecret configuration |
| Architecture and sequences | `SYSTEM_ARCHITECTURE.md` | Customer/admin/operator walkthrough |
| ER diagram | `CORE_ER.md` | Compare with `pg_dump --schema-only`, do not infer relationships |
| DB migration catalogue | `SCHEMA_INDEX.md` and `SCHEMA_MIGRATIONS_*.md` | `flyway_schema_history`, backup and restore rehearsal |
| Migration/backup operation | `DB_SCHEMA_AND_MIGRATION.md`, `migrate-safe.sh`, `db-introspection.sql` | Operator sign-off, rollback rehearsal |
| Every page/screen | `SCREEN_BY_SCREEN_UAT_MATRIX.md` | Web screenshots + signed Android APK results |
| KT demo | `KT_DEMO.md` | Owner, QA, finance, operations and support approvals |
| Production runbook | `../PRODUCTION_RUNBOOK.md` | Support escalation, alert thresholds, webhook and refund reconciliation |

## Refund-uncertain incident drill

1. A refund API request returns HTTP 503 with `code=REFUND_STATUS_UNCERTAIN`; record request/correlation ID, booking and payment references securely.
2. Show *unknown status*; do **not** claim refunded or invite blind retry.
3. Authorized finance staff inspect the Razorpay refund and transaction history, then reconcile against the local payment/refund records.
4. If provider already created a refund, record the existing provider reference and let normal status reconciliation advance. Do not submit another refund.
5. If provider proves no refund was accepted, follow the venue's approved retry/approval and idempotency process.
6. Capture test-mode evidence for webhook replay, partial deduction, no-show, and customer-visible booking status.

**Not certified by this document:** live UI screenshots, real payment gateway state, deployments, backup restore, secrets, production schema or signed APK. Those require explicit UAT execution and sign-off.
