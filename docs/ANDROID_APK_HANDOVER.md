# Jnapanam Sports Zone — urgent customer APK handover

## Goal
Create a directly installable Android APK for customer testing. This is **not** a verified production release or Google Play submission. Build and test the APK before sharing.

## Prerequisites
- Expo account, internet connection, Node.js 22, access to the repo.
- Release branch: `release/android-apk-handover-20261021`.
- A **reachable HTTPS** backend with successful `/actuator/health`. The previous Northflank service was crash-looping due to PostgreSQL configuration; fix this before a functional handover.
- EAS Build free-tier availability and queue times are not guaranteed.

## Build (Windows PowerShell)
```powershell
git clone https://github.com/bv-prasanna/jnapanam-sports-zone.git
cd jnapanam-sports-zone
git switch release/android-apk-handover-20261021
cd apps/mobile
npm ci
npx tsc --noEmit
npx eas-cli@latest login
npx eas-cli@latest init
```

In https://expo.dev , locate the linked project. Set the **preview** environment variable:
`EXPO_PUBLIC_API_URL=https://YOUR-WORKING-API-DOMAIN`

The URL must be the backend origin (Northflank API), **not** the Cloudflare frontend URL and **not** localhost. Do not embed a password, private token, or secret in any `EXPO_PUBLIC_*` variable. Expo public variables are compiled into the client.

Then:
```powershell
npx eas-cli@latest build --platform android --profile preview
```

Follow the EAS build link and download the generated **.apk** after status is Finished. Do not rename an .aab to .apk. Expo may request Android signing credentials; use an Expo-managed keystore for the preview APK and preserve signing continuity for future updates.

## Mandatory verification
1. Install the APK on a real Android phone (enable install from this source as prompted). Verify it opens without crashing.
2. Confirm `EXPO_PUBLIC_API_URL` points to a reachable backend and `/actuator/health` reports UP.
3. Register/login as customer; verify authenticated pages load.
4. Browse active sports/facilities and slots; test hold and booking confirmation using **test-mode payments only**.
5. Sign in as an operator and check today's bookings/check-in if the customer requests an operator preview.
6. Confirm permissions, app logo/name and Kannada/English text; perform one logout and relogin.
7. Record build URL, Git SHA, build timestamp and device tested. Share APK privately with customer only after passing smoke tests.

## Fast fallback
If EAS cloud build is blocked by Expo account access/queue, use a local Android Gradle/Android SDK environment with `npx expo prebuild --platform android` followed by a signed, tested release APK build. Do not share unsigned or untested binaries. An Expo Go link is **not** an APK.

## Critical notes
- Keep real payment collection disabled until backend, webhook and booking integrations have passed production testing.
- This build will use exactly the public API URL configured at build time; changing the server later may require an APK rebuild unless a stable API domain is used.
- Keep customer-facing demo accounts free of production personal data.
