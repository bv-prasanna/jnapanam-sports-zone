# Jnapanam Android APK release management

## Permanent link and QR
- Print only the QR in `apps/web/public/app-download-qr.svg`. It encodes **https://jnapanam.in/app**.
- Share **https://jnapanam.in/app** in WhatsApp, social media, posters and newsletters.
- The website queries `GET /api/v1/public/app-release/android` and links its Download button to `GET /api/v1/public/app-download/android`, which redirects to the admin-selected URL with `Cache-Control: no-store`.
- **An older QR that encodes the Google Drive URL cannot be changed remotely.** Replace it with the permanent Jnapanam QR in new materials.
- A Google Drive *view* URL may show Google's confirmation/download page. A direct-access HTTPS APK link on the allowed hosts is preferable.

## Admin: App Management
1. Build and test the new Android APK, note the *actual signed build versionCode* and signing certificate fingerprint.
2. Upload the APK to a public HTTPS Drive, GitHub Releases, Expo, Jnapanam or Cloudflare R2 destination.
3. Open Admin → **App Management**. Enter the download URL, latest version name (e.g. `1.1.0`), latest build number (`2`), minimum supported build (`1` initially), release notes and update mode.
4. First publish with **Off** (link only). Open `/app` on a phone, download APK and verify it installs.
5. Choose **Optional** once an updated APK is verified. Older clients that support the update gate show *Update / Later*.
6. Switch to **Mandatory** only when a replacement APK is verified and compatible. Set minimum supported build to the *new* versionCode (e.g. `2`). All version-aware Android clients below the minimum will display the update-required screen and receive HTTP 426 from protected APIs.
7. If rollout fails, set **Off** immediately. Changing the policy requires no APK rebuild. The API refreshes cached policy within 30 seconds.

## Client behavior
- Android version is determined by **expo-application nativeBuildVersion**; `android.versionCode` is the build-number source. Do not use user-facing `versionName` for minimum-version comparisons.
- Gate checks at app startup, when app resumes and after an HTTP 426.
- Current policy is saved in SecureStore so a previously known mandatory update is respected when offline.
- Mandatory mode prevents using the new-version-aware app until the installed native build meets the minimum.
- Optional mode allows **Later**.
- Backend filter checks `X-Jnapanam-Platform: android` and `X-Jnapanam-Version-Code: N`; it does not block web, CORS preflight or public update/download routes. Invalid/unrecognized headers cannot reliably identify older APKs.
- **Previous APKs built before the update checker and version headers cannot be forced to show an update screen** without installing a newer checker-enabled APK. The headers are a rollout/UX control, not an authorization boundary.

## Important: APK signing
The cloud-build workflow was previously creating a disposable signing key every time. Android cannot install a differently-signed APK over the existing app with the same package ID.

To enable reliable in-place upgrades, create/store **one persistent PKCS#12 signing keystore** and add these GitHub Actions repository secrets:

- `JNAPANAM_APK_KEYSTORE_BASE64` — base64-encoded .p12 bytes.
- `JNAPANAM_APK_STORE_PASSWORD` — keystore password.
- `JNAPANAM_APK_KEY_ALIAS` — alias.
- `JNAPANAM_APK_KEY_PASSWORD` — key password.

**Never commit the keystore or passwords.** Test the key locally or in staging before a production build. The APK workflow will use these secrets if *all four* are configured; if none are supplied it can still produce a disposable-key **UAT-only** APK with a warning and cannot guarantee install-over upgrades. Do not enable Mandatory when the user base cannot upgrade in-place.

The initial transition away from old disposable-signed UAT APKs may require users to uninstall manually and reinstall (losing local app data). Do not programmatically uninstall and do not ask customers to uninstall without explaining this risk. Existing server-side bookings/payments remain in the database; the app may need login again.

## Release safety
- Verify that the public APK is the signed correct version and that its SHA-256 matches the published artifact.
- QR and download URL should not point at random web pages. Server permits trusted public hosts only.
- Backend defaults to Off with no URL, avoiding outages on deployment.
- Stage the backend/web first, then deploy compatible signed Android APK, verify check-in and Razorpay payment return paths, then enable Optional and finally Mandatory.
