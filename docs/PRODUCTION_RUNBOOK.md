# Production operations runbook

## Required production configuration
Set strong `JWT_SECRET`, production PostgreSQL credentials, restrictive `CORS_ALLOWED_ORIGINS`, payment provider secrets, and `NOTIFICATION_PROVIDER_URL/TOKEN`. Keep `EXPOSE_DEVELOPMENT_RESET_TOKEN=false`.

## Monitoring
Backend exposes Spring Boot Actuator `/actuator/health`, `/actuator/info`, and `/actuator/metrics`. Production monitoring should probe health every minute and alert after consecutive failures. Track HTTP 5xx rate, JVM memory, DB connection pool, payment webhook failures and notification delivery failures.

## Database backups
Run a PostgreSQL logical backup at least daily and before every production migration:
```bash
pg_dump --format=custom --no-owner --dbname="$DB_URL" --file="jnapanam-$(date +%Y%m%d-%H%M).dump"
```
Encrypt backups at rest, copy them to separate durable storage, retain daily/weekly/monthly generations according to the owner's policy, and test restoration regularly.

Restore rehearsal:
```bash
pg_restore --clean --if-exists --no-owner --dbname="$RESTORE_DB_URL" jnapanam-YYYYMMDD-HHMM.dump
```

## Deployment gate
1. Application CI green on the exact release commit.
2. Database backup completed.
3. Flyway migration validated on staging/restore database.
4. Health endpoint green after deployment.
5. Smoke-test login/MFA, booking/payment, operations check-in, membership, training, tournament and shop.
6. Verify notification provider and payment webhooks.
7. Record deployment SHA and rollback point.

## Rollback
Application rollback is to the previous known-good image/SHA. Database migrations are forward-only; restore from the pre-deployment backup when a migration cannot be safely forward-fixed.


## Automated backup job
Schedule `scripts/production-backup.sh` daily in the production scheduler with DB_URL, DB_USERNAME and PGPASSWORD secrets. Retain at least 14 days, copy backups to durable off-host/object storage, and perform a monthly restore rehearsal. `/actuator/health` is the deployment and uptime probe; alert on non-2xx health responses, elevated 5xx rate, DB connection failures and backup-job failures.
