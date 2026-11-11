-- Preserve the exact configured tax rate and inclusion mode on each captured booking.
ALTER TABLE bookings
 ADD COLUMN tax_rate_percent numeric(7,3),
 ADD COLUMN tax_inclusive boolean;
