# Jnapanam — Full Migration Documentation Index

> All **60** default-branch Flyway SQL files were examined in five ordered groups on 2026-11-09. These are code-derived schema notes, **not a reconstruction of the final deployed database**.

| Coverage | Migration sequence | Document |
|---|---|---|
| 1 | 1–14 | [Tables, ER, columns and ALTER notes](SCHEMA_MIGRATIONS_01.md) |
| 2 | 15–28 | [Tables, ER, columns and ALTER notes](SCHEMA_MIGRATIONS_02.md) |
| 3 | 29–43 (V42 absent) | [Tables, ER, columns and ALTER notes](SCHEMA_MIGRATIONS_03.md) |
| 4 | 44–57 | [Tables, ER, columns and ALTER notes](SCHEMA_MIGRATIONS_04.md) |
| 5 | 58–61 | [Tables, ER, columns and ALTER notes](SCHEMA_MIGRATIONS_05.md) |

See [logical architecture, deployment, ER overview, KT and UAT](README.md) and [guarded migration script](migrate-safe.sh).

## Production database reconciliation

1. Record the exact running backend commit SHA and Spring Boot/Flyway version.
2. Verify the target database and read-only role.
3. Inspect Flyway `flyway_schema_history`, failed/pending scripts and checksum drift.
4. Export actual schema with `pg_dump --schema-only --no-owner --no-privileges` to a controlled location.
5. Reconcile columns, indexes, constraints, views, triggers and foreign keys against the source SQL.
6. Check dependencies used by booking/payment/refund, pricing, membership, operator and notification journeys.
7. Approve diagram as **physical** only after developer and DBA signoff. Never infer that source inspections prove the production schema.

## Release and KT sign-off
Use the [technical handover](README.md) for full demo and negative tests. Demonstrate a real staging backup/restore, approved migration, slot booking, sandbox payment, partial refund, admin/operator actions, logs and rollback. Record UAT evidence in a tracked release issue.

**Source limitations:** migrations can alter or drop prior tables; part diagrams are per-file-group snapshots, not a single cumulative ER, and SQL scanners are conservative.