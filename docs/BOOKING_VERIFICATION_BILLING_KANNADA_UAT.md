# Jnapanam booking verification and billing UAT

## Booking eligibility
- NOT_SUBMITTED: cannot hold or confirm a new booking. Upload supported DL/Voter ID/Passport.
- PENDING: a document exists, is awaiting Admin approval, and a booking is allowed while pending; customer sees “Verification pending”, never the old first-time upload prompt.
- APPROVED: booking allowed; customer sees verified status.
- REJECTED: all new bookings blocked, even for returning users. Show review reason when available; customer must upload a corrected document; the new upload becomes PENDING.
- Backend enforces policy for **every** new hold and booking confirmation, not only the user's first booking.
- Existing confirmed bookings are not deleted because of later document decisions.

## Facility terms (Kannada)
- Each facility stores independent English `termsAndConditions` and Kannada `termsAndConditionsKn` values.
- For baseline hygiene rules, V58 seeds the corresponding Kannada text. Admin can edit custom Kannada terms in Facility editor.
- UI shows Kannada text when Kannada is selected; if a custom translation is unavailable, it falls back to stored English without inventing legal wording. Consent remains server-validated, with the English snapshot in the booking audit record.

## Billing format
- GST invoice and credit notes are issued from immutable stored taxable/CGST/SGST/IGST snapshots and include the original service price, coupon/promo code, discounts, loyalty points and loyalty tender, captured cash and invoice total when present.
- Payment receipts are **not** labelled GST invoices and may show tax only as a labeled snapshot; they show collected payment, processed refunds and net retained.
- UI and PDF list all available monetary fields; no fake taxes are generated for missing snapshots.
- UAT: print an invoice for a payment with promotion+loyalty+GST; check sum, invoice number, seller GSTIN/state, customer address, and PDF legibility on A4.

## Release
Deploy backend before web, since the web now expects verification state and Kannada terms. Flyway V58 is additive.
Mobile code is included but do NOT build APK until owner explicitly requests.
