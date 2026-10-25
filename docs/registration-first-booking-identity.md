# Registration and first-booking identity workflow

Status: product requirement, **not yet implemented as document storage or booking gate**.

## Registration
- Required: full name, email address, password.
- Email ownership: one-time code to provided email before account is created.
- Optional: mobile number (unverified), postal address (editable later in My Profile).
- Do not request identity document during registration.
- Do not consider email verification proof of mobile ownership.
- Do not allow unverified mobile number to be used for account recovery.

## First booking
- Before submitting the **first booking**, request one identity/address proof document.
- Accepted address proof options: **Driving Licence (DL), Voter ID, or Passport only**. The document must show the customer's address. One document is sufficient. Do not accept PAN, Aadhaar, utility bills, or property tax receipts for this flow.
- Display purpose, access restrictions and retention notice; allow the customer to correct/change document through My Profile.
- Upload must be authenticated, encrypted at rest, size and MIME validated, malware scanned, accessible only to customer and approved verification staff, audit logged, and never served through a public URL.
- Add verification status `NOT_SUBMITTED / PENDING / APPROVED / REJECTED`. Backend should enforce a first-booking eligibility policy at both hold and confirmation endpoints, not just in the UI.
- Define how soon after upload a booking may proceed: customer-friendly option is permit `PENDING` temporarily with a clear verification deadline before check-in; otherwise approvals could block all booking UAT.
- Users under 18 need guardian-controlled submission and access.
- Decide retention/deletion policy and avoid collecting PAN images unless genuinely required.

## Login / trusted device
- Email or mobile + password is allowed only for identifiers already linked to the account.
- New/untrusted device: require an actual possession-factor OTP to a verified email. Mobile OTP may follow later.
- Subsequent use: use revocable, rotating refresh token stored in platform secure storage bound to a cryptographically generated device credential. Device fingerprint alone is **not authentication**.
- Reauthentication after password changes, token revocation, device loss, or unusual activity; user may revoke trusted devices from My Profile.

## Current implementation
- Email OTP registration is under PR #51, not yet built/deployed.
- Email/mobile identifier login is in a separate development branch, not released.
- Document upload, address/profile editing, verification workflow, and mandatory first-booking gate are **pending**.
