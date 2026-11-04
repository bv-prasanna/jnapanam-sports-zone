import { Redirect } from 'expo-router';

// The hosted Razorpay checkout returns here only after server verification.
// The Bookings screen reloads authoritative booking state when mounted.
// This route never marks a payment successful by itself.
export default function PaymentReturn() {
  return <Redirect href="/bookings" />;
}
