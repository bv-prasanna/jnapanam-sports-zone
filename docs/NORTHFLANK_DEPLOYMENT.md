# Jnapanam Sports Zone — Northflank Sandbox deployment

## Scope and cost
The Northflank Sandbox currently advertises two free services, one free database and two free cron jobs. **Sandbox is for development/pilot evaluation; not guaranteed production uptime, storage capacity or no future plan changes.** Verify resource limits, region, billing settings and database backups before enabling real bookings or payments.

Architecture: Cloudflare hosts the web frontend; Northflank hosts a Spring Boot Java 21 API in a combined service and a private PostgreSQL add-on. Customer/operator mobile clients call the same HTTPS backend URL.

## Step 1 — GitHub and branch
Use the repository `bv-prasanna/jnapanam-sports-zone`. Merge the deployment PR only after its CI succeeds. The Release 1 functionality PR is not automatically production-ready. Point Northflank at `main` after approved merge (or select the deployment branch for a throwaway test deployment).

## Step 2 — Create Northflank project and PostgreSQL
1. Create/log in at https://app.northflank.com and link GitHub with **repository-scoped** access.
2. Create a new project. Select a supported region with suitable network latency.
3. Add a PostgreSQL database using the **free Sandbox resource** if offered. Use a strong randomly generated database password. Do not enable public DB access unless absolutely necessary.
4. In database connection details, copy the **private/internal** host, port, database name, user and password. Create the JDBC URL `jdbc:postgresql://HOST:PORT/DB_NAME?sslmode=require` **only if the internal endpoint supports TLS**. Otherwise follow the endpoint's documented TLS mode. Do not copy a PostgreSQL `postgres://` URL directly into `DB_URL`.
5. Store the DB credentials only in Northflank runtime secrets; keep them out of Git.

## Step 3 — Create combined backend service
1. Select **Create new → Service → Combined**.
2. Repository: `bv-prasanna/jnapanam-sports-zone`; build branch: approved branch (ultimately `main`).
3. Build type **Dockerfile**; Dockerfile location **/apps/backend/Dockerfile**; build context **/apps/backend**. Enable BuildKit caching when offered.
4. Choose an available free Sandbox compute plan. Set 1 replica; **do not accidentally select pay-as-you-go compute**.
5. Expose a public **HTTP** port `8080`. Northflank handles HTTPS termination at the public endpoint.
6. Add the runtime variables from `apps/backend/.env.northflank.example`. Use a project/service secret group if preferred. Set `SPRING_PROFILES_ACTIVE=prod`, `EXPOSE_DEVELOPMENT_RESET_TOKEN=false`, a **unique Base64-encoded JWT secret**, restrictive production CORS origin(s), and DB credentials.
7. Never enable test OTP, password reset token disclosure or permissive CORS in the production profile. Don't ship default local database credentials or JWT secret.
8. Create the service and watch build and startup logs. Flyway runs at startup and validates schema against Hibernate.

## Step 4 — Health checks
Configure Northflank HTTP readiness/liveness probe:
- protocol: HTTP
- path: `/actuator/health`
- port: `8080`
- initial delay: **90 seconds** (allow first-time Flyway migration and JVM startup)
- interval: **30 seconds**
- timeout: **10 seconds**
- failure threshold: **3** (if the UI exposes these settings)

Expected: HTTP 200 with JSON `{"status":"UP"}`. If endpoint returns 401/403 verify the existing Spring Security actuator matcher. If status is DOWN inspect DB/Flyway logs before trying restarts. The base image does not require a shell-based health command.

## Step 5 — Connect frontend/mobile
- Configure Cloudflare web app's public API base URL to the Northflank HTTPS service URL, at the right build/runtime configuration point for the Next.js worker.
- Set `CORS_ALLOWED_ORIGINS` to the exact Cloudflare frontend origin(s), comma separated and without paths.
- Configure mobile app's production API URL to the same Northflank HTTPS endpoint, rebuild for mobile release. Do not use localhost for deployed mobile clients.
- Never put JWT secret, database password, payment secret or notification API keys in client build variables.

## Step 6 — Quality and rollout gates
1. Run the repository's Application CI backend integration tests and web/mobile checks; run **Northflank Docker Build** workflow on the same release commit.
2. Verify `GET /actuator/health` is UP and there are no Flyway failures.
3. Test customer registration/login, OTP, password reset, roles/permissions, booking holds and concurrent bookings, cancel/reschedule, payment, operator check-in/out, memberships, training, tournament and shop.
4. Verify notification **in-app** delivery and separately validate external SMTP/SMS integrations only when credentials are configured.
5. Confirm payment provider and webhooks before accepting real payment. `PAYMENT_PROVIDER=mock` is **not** appropriate for live payments.
6. Make a PostgreSQL backup and rehearse a restore to a separate database before public launch; check actual Sandbox backup/retention restrictions.
7. Verify free resource usage and memory/CPU. A Java 21/Spring Boot + PostgreSQL deployment might exceed available Sandbox resources; if so, do not silently upgrade to a paid plan.

## CI/CD and rollout
The existing `.github/workflows/application-ci.yml` runs tests on PRs; `.github/workflows/northflank-docker-ci.yml` builds the Docker image for PRs/main without pushing or deploying. Northflank **Combined service** can auto-build and auto-deploy from the selected branch after Git integration. For initial pilot prefer manual promotion/approved main-only changes. GitHub tests and Northflank deploy are separate: block merging unless required checks pass.

## Rollback and migration
Keep the previous working commit/image and deploy it via Northflank's service rollback controls when possible. Flyway migrations can be irreversible: database rollback requires a compatible backup and restore plan. For AWS/Azure later, reuse the Dockerfile and standard JDBC configuration.

## Troubleshooting
- Build fails: validate Dockerfile/context paths; check Java/Maven errors from build logs.
- Health DOWN: validate `DB_URL` format, DNS, TLS mode, credentials and Flyway migration.
- OOMKilled: inspect limits and heap; reduce memory utilization, or move to a suitably sized provider.
- FE CORS error: confirm exact Cloudflare origin and redeploy backend.
- Login fails: check JWT secret, clock skew and prod/test auth settings; never enable test OTP as a fix.
- Email/SMS absent: in-app notifications do not prove external provider delivery. Inspect provider credentials and delivery logs.

Reference: https://northflank.com/docs/v1/application/getting-started/build-and-deploy-your-code
Northflank Dockerfile docs: https://northflank.com/docs/v1/application/build/build-with-a-dockerfile
Pricing: https://northflank.com/pricing
