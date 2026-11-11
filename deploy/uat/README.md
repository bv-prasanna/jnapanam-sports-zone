# Jnapanam UAT launch — isolated from production

## Target URLs
- Frontend: https://uat.jnapanam.in (new Cloudflare Workers custom domain)
- Backend: separate **jnapanam-uat-api** Northflank service public HTTPS URL; never reuse production backend URL
- Database: separate **jnapanam-uat-postgres** database/service; do not point UAT at production Postgres
- Production: https://jnapanam.in remains unchanged.

## Northflank — new UAT backend service
1. Create an independent Northflank service from GitHub repository `bv-prasanna/jnapanam-sports-zone`, branch `feat/release1-requirements-completion`.
2. Build context: `/apps/backend`; Dockerfile: `/apps/backend/Dockerfile`. Expose port 8080 (HTTP).
3. Provision a **separate** PostgreSQL 17 addon, and set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` to its internal connection values.
4. Set environment values as in `deploy/uat/backend.env.example`, replacing placeholders with Northflank secrets.
5. Use a brand-new random 32+ byte JWT secret for UAT; never copy prod JWT key.
6. Do not configure a live SMS account for test registrations. Only allowlisted test numbers skip the SMS provider. Non-allowlisted numbers will still call Fast2SMS and can fail.
7. Require authenticated admin access; protect UAT with a Cloudflare Access policy (or another account-authenticated perimeter), so the generic public Internet cannot access it.
8. Verify `/actuator/health` returns `UP`; verify Flyway has run. Verify that `071977` works only for allowlisted test number and only **after** requesting a challenge, and non-allowlisted numbers cannot use it.

## Cloudflare — frontend
1. Create a **separate** Cloudflare Worker named `jnapanam-uat` using the same `apps/web` project source.
2. Build with `NEXT_PUBLIC_API_URL` set to the new UAT backend HTTPS origin. This value is compiled into the client bundle; changing it later requires rebuilding the Worker.
3. Add `uat.jnapanam.in` as the UAT Worker's custom domain; ensure it does not route to the prod Worker.
4. Put both UAT frontend and backend behind authenticated access. CORS on the UAT backend should allow only `https://uat.jnapanam.in`.
5. Test that UAT frontend calls the UAT API rather than production using the browser's Network panel.

## Seed and testing
- Use only fake users and dummy facility/bookings. Never import real production phone numbers, payment credentials or customer personal data.
- With `TEST_OTP_ENABLED=true`, `TEST_OTP=071977`, and an explicit comma-separated `TEST_OTP_ALLOWED_MOBILES`, registration OTP is skipped **only** for those numbers.
- On the public production backend keep `TEST_OTP_ENABLED=false` and `SPRING_PROFILES_ACTIVE=prod`.
- The separately drafted Super Admin allowlist UI is in PR #48 and needs merge/testing before being available in UAT.
- For APK testing, build the existing mobile workflow with `workflow_dispatch` input `api_url=<UAT API HTTPS URL>`; the production APK points to the production API.
- Payments must remain mock-mode, outbound email/SMS/WhatsApp disabled or sent only to explicit test recipients.
- Validate registration, login, booking, check-in/out, location auditing, player growth, admin roles and notifications before any production rollout.

## Cutover
Never promote a UAT database into production. After UAT acceptance, merge tested code into production branch and deploy normally, with real OTP provider and `TEST_OTP_ENABLED=false`.
