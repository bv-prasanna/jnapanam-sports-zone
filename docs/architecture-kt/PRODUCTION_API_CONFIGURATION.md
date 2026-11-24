# Production API configuration

The web and Android clients must use the deployed HTTPS backend origin. Localhost and Android-emulator hostnames are development-only.

## Web (Cloudflare Pages)

Set the build-time variable for the production Pages environment:

- `NEXT_PUBLIC_API_URL=https://<your-production-backend-host>`

Use the preview backend origin in the preview environment. Do not include an API path suffix such as `/api/v1`; the clients append API routes themselves. Redeploy after changing a build-time public variable.

If the production variable is missing, the web API client now fails with a clear configuration message instead of attempting to call the visitor's own `localhost:8080`.

## Android (EAS)

Set `EXPO_PUBLIC_API_URL` in both EAS environments used by the build profiles:

- `preview`: the HTTPS UAT backend origin
- `production`: the HTTPS production backend origin

The EAS profiles in `apps/mobile/eas.json` select these named environments. Release builds no longer fall back to `10.0.2.2` or `localhost`; a missing URL produces a safe configuration error before network requests. Expo development mode retains the local emulator fallback.

## Verification and release gate

1. Confirm the variable exists in the correct hosting/EAS environment without printing its value in CI logs.
2. Build the web app and Android preview/release artifact using that environment.
3. Inspect the built client configuration to ensure it contains the intended HTTPS backend origin and no localhost URL.
4. Test login, token refresh, catalogue, booking, payment return and session expiry against the intended environment.
5. Never treat a successful Docker build as a deployment. Confirm the backend deployment and health check separately.

The repository does not contain production secret values. Configure environment variables in the hosting providers; do not commit credentials or production endpoints containing secrets.
