# Jnapanam — Database schema, migration plan and safe execution

## Sources of truth

- **Schema migrations:** apps/backend/src/main/resources/db/migration/V1__...sql through V61__...sql.
- **Count:** 60 Flyway SQL migration files on main when reviewed (V42 is absent; investigate deployment history, but a sequence gap alone does not imply corruption).
- **Runtime:** Spring Boot PostgreSQL; JPA ddl-auto=validate; Flyway enabled in application.yml.
- **Catalogue:** SCHEMA_INDEX.md and five SCHEMA_MIGRATIONS_*.md documents (new tables, ALTER statements, PK/FK notes).
- **Only physical truth:** the database catalog and flyway_schema_history on the verified environment.

## Mandatory no-surprise migration process

1. Choose DEV, UAT or PROD and verify DB host/database via a **read-only** account; obtain change ticket and approver. Never post DB credentials to GitHub.
2. Check current release SHA, app build, pending Flyway scripts, prior failed migrations and drift from the original applied-script checksums.
3. Capture encrypted backup with pg_dump and validate a restore to an isolated temporary database.
4. Run ../../scripts/verify-flyway-source.sh plan to check source versions and detect duplicates; review output.
5. On staging: validate against matching Flyway version and DB extension configuration, apply pending migration, run /actuator/health and full booking/payment regression.
6. For PROD: gate app writes and webhook handling during schema changes as appropriate, record approved backup reference; use the existing guarded migrate-safe.sh **only with a reviewed migration command** or the deployment platform's authorized Flyway run. Do not run concurrent startup migration processes.
7. After: inspect flyway_schema_history, constraints, index validity, test one booking and one refund in approved test mode; verify audit records and record approval.
8. If migration fails: stop deployment; do not edit applied SQL, blindly run Flyway repair, or repeatedly retry financial operations. Use a reviewed forward-fix or tested restore.

## Read-only schema inventory

The repository script db-introspection.sql queries flyway_schema_history, table/column metadata, FK constraints and indexes without reading customer records. Use only after verifying the database target:

```bash
psql --set ON_ERROR_STOP=1 --dbname="$PG_READONLY_URL" --file=docs/architecture-kt/db-introspection.sql > schema-audit.txt
```

**Do not share schema output publicly without review; table names and indexes may expose sensitive implementation details.**

## Backup examples

```bash
# Set PG_BACKUP_URL privately to a verified postgresql:// connection string.
pg_dump --format=custom --no-owner --no-privileges --dbname="$PG_BACKUP_URL" --file=jnapanam-prechange.dump
# Restore only into a separately provisioned disposable DB; never overwrite production:
pg_restore --no-owner --no-privileges --dbname="$PG_RESTORE_TEST_URL" jnapanam-prechange.dump
```

Keep backup artifacts encrypted in a different failure domain; record owner, duration, retention and successful restore evidence.

## Adding a new database requirement

Use a **new** Flyway version above the highest applied number. An example template below is deliberately non-executable to prevent accidental writes:

```sql
-- EXAMPLE ONLY: apps/backend/src/main/resources/db/migration/V62__descriptive_change.sql
-- Preflight: obtain owner/DBA approval, plan table lock duration, backfill strategy, rollback procedure.
-- Use ALTER TABLE / CREATE INDEX with reviewed constraints and tests.
-- For large indexes or online migrations, check Flyway transaction semantics.
```

Because the changes in this review affect Java request metadata and documentation only, **no new SQL migration is required or supplied**.
