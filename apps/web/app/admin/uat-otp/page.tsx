import {notFound} from "next/navigation";

// Fixed-OTP administration is not exposed from the live production website.
// Keep the backend test-OTP switch disabled in production configuration.
export default function DisabledUatOtpPage(){notFound();}
