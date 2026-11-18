# Jnapanam iPhone release and customer UAT (TestFlight)

## Current implementation

- The mobile client is a shared Expo Router / React Native application in `apps/mobile`.
- `apps/mobile/app.json` already declares iOS bundle ID `com.jnapanam.sportszone`, iOS icon, portrait orientation, location, camera/QR and photo-library permission descriptions.
- Android remains on the existing APK/AAB pipeline. iOS uses the new `ios-uat` EAS build profile below.
- This is one cross-platform codebase; do not fork a second iOS app.

## Accounts and prerequisites

1. The customer/owner must enroll in the Apple Developer Program (organization enrollment is preferable if the app belongs to a registered business). Apple charges an annual membership fee; confirm the current fee and legal-entity requirements directly with Apple before purchase.
2. Create the app record in App Store Connect with bundle ID exactly `com.jnapanam.sportszone`; choose the final public seller name and primary category.
3. Create/link the Expo EAS project from `apps/mobile` using the owner's Expo account: `npx eas-cli@latest init`. This writes the EAS project ID into Expo config; commit that generated project ID (it is not a secret).
4. Configure iOS Distribution certificate and provisioning profile in EAS credentials for the same Apple Developer team. Keep Apple credentials, certificates and API keys in EAS/secret storage; never commit them.
5. Configure App Store Connect API access in EAS Submit credentials for automated submission, or upload the generated IPA using Transporter. The Apple Account holder must accept current agreements and complete tax/banking/compliance setup before public release.

## Build an iPhone UAT build

From repository root:

```bash
cd apps/mobile
npm ci
npx tsc --noEmit
npx eas-cli@latest build --platform ios --profile ios-uat
```

- The first run may ask to create/link the EAS project and configure Apple signing. Complete that once with the owner's accounts.
- EAS runs the iOS build on its hosted macOS infrastructure; a local Mac is not required for cloud builds.
- Download the generated `.ipa` from the EAS build page or use EAS Submit to upload it to App Store Connect.
- In App Store Connect, add internal testers in TestFlight; for external customer testers, submit the build for Apple's TestFlight Beta App Review when required.
- Invite customer testers by email or public link after processing is complete. They install TestFlight from the App Store and then install Jnapanam.

## Optional automated TestFlight upload

After App Store Connect API credentials are configured in EAS:

```bash
cd apps/mobile
npx eas-cli@latest submit --platform ios --profile ios-uat --latest
```

Upload does not itself release the app publicly. The build first appears in TestFlight after Apple's processing. TestFlight availability and beta review are controlled by Apple.

## iPhone UAT test matrix

Run on at least one supported physical iPhone and the oldest iOS version the product intends to support:

- Install, first launch, update install, app resume, force-close/relaunch and logout.
- Customer registration/login, email MFA where enabled, expired session and secure token persistence.
- Venue/service discovery, Kannada/English switch, booking, capacity race, payment pending/success/failure and server-verified booking status.
- QR display and staff QR scan/check-in; camera denied, permission later granted, invalid/expired/replayed QR.
- Profile image selection/camera denied; location denied and continue without location.
- Membership/subscription, shop/counter where role-enabled, cancellation, refund pending/uncertain and server reconciliation.
- Push/deep-link/payment return behavior where configured; universal links must be associated with the production domain before relying on them.
- iPhone safe areas/notch/Dynamic Island, keyboard avoiding inputs, scrolling, 44pt touch targets, text scaling, VoiceOver labels, dark/light mode and network interruption.
- Confirm iOS app does not expose Android-only download/install instructions; links should direct iPhone users to App Store/TestFlight.
- Confirm all roles and feature access against backend authorization, not just hidden mobile controls.

Record device model, iOS version, app version/build number, account role, backend environment and screenshots for every UAT case.

## App Store privacy and release readiness

Before external TestFlight/public release, complete App Store privacy details using actual SDK/data behavior: account/contact details, profile images, location (only when granted), diagnostics and any payment-related data. Provide privacy policy and support URL, account deletion workflow if accounts can be created, age rating, export-compliance answers, screenshots and review notes. Validate camera/photo/location usage strings against actual behavior. Do not claim payment-card data is collected by Jnapanam if hosted Razorpay checkout handles it; verify actual data flow first.

## Rollback and versioning

- Keep Android versionCode and iOS build number monotonic; EAS `autoIncrement` is enabled for the iOS UAT profile.
- TestFlight build replacement is a new build, not an in-place rollback. If a serious defect is found, stop tester rollout, fix, build a higher build number and distribute a replacement.
- Do not publish to the public App Store until customer UAT sign-off and owner approval.

## Not yet completed

No Apple Developer/App Store Connect credentials are available to this repository review. Therefore no signed iOS IPA, TestFlight upload, Apple review or physical-iPhone validation is claimed complete. The customer/owner must provide and authorize the Apple accounts and credentials before those steps can be performed.
