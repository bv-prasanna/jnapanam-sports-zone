# Jnapanam — Deployment, operations and knowledge-transfer demonstration

> Proposed demo script. Run only in a protected UAT environment with fictional users and sandbox transactions.

## Audience and preparation

Attendees: Super Admin, Admin, Operations/Front Desk, Shop Keeper, Finance, venue owner, IT/DBA and support. Before the meeting record staging URL, branch and SHA, signed Android version, seeded facility/sports, demo identities for each role, sandbox Razorpay keys, SMTP/SMS provider, audit access and a backup reference. Never display real secrets, invoices with personal data, OTPs or unmasked customer addresses.

## 60–90-minute walkthrough

| Section | Demo | Evidence to retain |
|---|---|---|
| 1. Architecture (5m) | Explain DEPLOYMENT_TOPOLOGY.md, SYSTEM_ARCHITECTURE.md and ER_CORE.md | Deployment SHA / diagram version |
| 2. Access (10m) | Register / login / MFA, wrong password, session refresh, role-specific sidebars | 401/403/MFA code scenarios and audit ID |
| 3. Catalog (10m) | Sport, facility, multiple weekly hours, date override, blackout and booking capacity | Admin edit + public slot preview |
| 4. Booking (10m) | Customer hold, confirm, PAY_NOW/PAY_AT_VENUE, booking details and cancellation | Booking number, price/tax snapshot |
| 5. Payments (10m) | Sandbox checkout, signed capture webhook, duplicate webhook, failed payment and timeout | Gateway ID, backend status, receipt |
| 6. Refund (10m) | Future cancel with retained fee, past no-show discretionary partial refund, uncertain refund reconciliation | Reason + deduction, final gateway state |
| 7. Venue ops (10m) | Operator slot board, QR/photo check-in, check-out/no-show and onsite payment reconciliation | Audit action with facility and actor |
| 8. Adjacent flows (10m) | Membership, coaching attendance, tournaments, shop, tax billing and admin report | Role-authorized report / invoice |
| 9. Support (5m) | Force sandbox error, show sanitized admin error log and correlation reference | Request ID + HTTP category |
| 10. Handover (5m) | Validate Flyway history, backup/restore record, Android release gate, rollback roles | Signed support runbook checklist |

## Deployment and maintenance SOP

- Daily: check API health, payment/refund mismatch queue, declined webhook delivery, notification outbox, booking capacity reconciliation and error 5xx rate.
- Weekly: confirm backup completion, expired holds, unresolved failed refunds, admin role audit, signed APK distribution and open customer support issues.
- Monthly: restore rehearsal, dependency/security upgrades, access review, MFA and password-reset tests, performance and accessibility QA.
- Every release: record semantic version, Git SHA, Flyway baseline/target, backend/web/Android artifact IDs, deployed URLs, CI run URL, security checklist, migration approval, QA evidence and sign-off.
- Change management: never edit applied migrations; test forward-fix and restore; avoid running live customer refunds during deployment window unless the gateway state is monitored.

## UAT acceptance record

- Customer owner: __________________  Date: __________
- Venue operations lead: ____________  Date: __________
- Finance/refund approver: ___________  Date: __________
- Support/on-call owner: _____________ Date: __________
- Technical maintainer: _____________ Date: __________
- Deployment SHA / DB migration: _______________________
- Known defects / accepted limitations: _________________
- Final gate: **UAT approved / conditional / rejected** (select and attach test evidence)

## Linked material

- [Deployment](DEPLOYMENT_TOPOLOGY.md)
- [Architecture](SYSTEM_ARCHITECTURE.md)
- [ER overview](CORE_ER.md)
- [Physical schema audit and migration](DB_SCHEMA_AND_MIGRATION.md)
- [HTTP, security, UI checklist](HTTP_AUDIT_UI_CHECKLIST.md)
- [Migration catalog](SCHEMA_INDEX.md)
- [Production runbook](../PRODUCTION_RUNBOOK.md)
- [Payment UAT](../REFUND_ADJUSTMENTS_UAT.md)
