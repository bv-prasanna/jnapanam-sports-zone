# Facility slot rules, booking acknowledgement and feature visibility

## Admin configuration

- **Facilities > Edit:** `capacity` is the maximum total participants in each bookable slot (e.g. 20 swimming guests). `slotDurationMinutes` is activity time, and `bufferMinutes` is a non-bookable changeover/cleaning interval. `termsAndConditions` is the facility-specific text that customers must accept when booking.
- **Facilities > Hours & closures:** Each weekday can contain multiple windows, e.g. 09:00–17:00 and 19:00–21:00. Use *Add another period* and save. Closed days have no opening period.
- **Admin > Feature Visibility:** Switch customer-facing features on/off, then select allowed roles and save. Training defaults to disabled for the customer; admin training management remains accessible.

## Example

Swimming capacity: 20, activity: 55 min, cleaning buffer: 5 min, Monday windows 09:00–17:00 and 19:00–21:00. This generates eight morning/day slots and two evening slots; each allows 20 participant bookings. Booking party-size/quantity consumes that many places from the same slot.

## API

- `PUT /api/v1/admin/catalog/facilities/{id}/operating-periods/{DAY_OF_WEEK}` replaces all the windows for one weekday **atomically**. Body: array of `{dayOfWeek,openTime,closeTime,closed:false}`. For closed days send one entry with `closed:true` and null times.
- Legacy single window `PUT .../operating-hours` remains as a replace-single-period compatibility route.
- `GET /api/v1/admin/catalog/facilities/{id}/schedule` returns separate `weeklyOperatingHours` entries for all open windows.
- `POST /api/v1/bookings/confirm` supports `acceptedTerms:true`. Confirmation requires acceptance when a facility has terms. Booking records snapshot the exact rules and timestamp.
- `GET /api/v1/public/features` lists current feature switches/role access. Authorized administrators can GET/PUT `/api/v1/admin/features/{CODE}`.

## Safety and future reservations

Existing booked/held slots are not silently resized or moved when a facility's slot duration, buffer or opening windows change; changed slots are withheld until their existing reservations are released. The calendar regenerates unoccupied slots on next availability lookup. This protects live customer reservations.

**Deployment:** DB migrations V56 and V57 must be applied before the new web/mobile clients are used. Coordinate backend+web+APK rollout to prevent legacy booking clients from failing newly required terms acceptance. Tests and UAT must cover concurrency, price/payment, slot overrides, closure, and expired holds.
