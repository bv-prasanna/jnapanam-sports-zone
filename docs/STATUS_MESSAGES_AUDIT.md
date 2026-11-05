# Jnapanam user status messaging — code audit

Messages must represent confirmed backend state, not a tap, attempted payment, browser return, or successful list refresh.

| Operation | Show to user | Meaning |
| --- | --- | --- |
| Invalid login | Invalid credentials or enter MFA code, not session expired | No authenticated session established |
| Booking PAY_AT_VENUE | Booking confirmed; payment due at venue | Reservation created, payment outstanding |
| Booking PAY_NOW | Payment successful only after backend verification or confirmed receipt | Captured payment |
| Browser/UPI incomplete or failed | Payment not confirmed; check status before retrying | Money not proved captured |
| Shop pickup checkout | Order placed; payment due at pickup | Order created, funds not collected |
| Venue check-in | Check-in confirmed | Check-in API committed |
| Operator check-in, checkout, no-show, cancel | Action recorded | Operation API committed |
| Operator onsite collection | Payment recorded; confirm funds received | Staff-recorded payment, not independent UPI capture verification |
| Cancel-and-refund | Cancellation saved, refund request initiated; verify final refund status | Refund not necessarily processed |
| Refresh fails after write | Action recorded, list could not refresh | Write success and independent read failure |
| Backend error | Safe message and support reference | An API request failed, not a business success |

Existing payment service sends a success notification only after a verified payment has been recorded; late capture against an expired booking is held for review. The client code now preserves confirmed status banners after refresh and adds operator confirmations in English and Kannada.

Automated checks cover these contracts. Further live-device testing is required for Razorpay, notification delivery and all lifecycle variants. Admin error logs record failed API status and request references; successful modifying requests are recorded by the existing audit filter. External notification delivery status must be checked separately.
