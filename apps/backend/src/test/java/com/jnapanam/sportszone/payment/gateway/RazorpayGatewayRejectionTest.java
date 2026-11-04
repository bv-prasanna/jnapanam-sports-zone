package com.jnapanam.sportszone.payment.gateway;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RazorpayGatewayRejectionTest {
 @Test void rejectedPaymentReturnsSafeActionableText() {
  assertTrue(RazorpayGateway.rejectionMessage(401).contains("credentials"));
  assertTrue(RazorpayGateway.rejectionMessage(403).contains("merchant account"));
  assertTrue(RazorpayGateway.rejectionMessage(400).contains("booking number"));
  assertFalse(RazorpayGateway.rejectionMessage(500).contains("Authorization"));
 }
 @Test void razorpayDiagnosticTokenMustNotLogUntrustedDescriptionOrCredentials(){
  assertEquals("BAD_REQUEST_ERROR",RazorpayGateway.safeToken("BAD_REQUEST_ERROR"));
  assertEquals("unavailable",RazorpayGateway.safeToken("A sensitive error with spaces or secrets"));
  assertEquals("unavailable",RazorpayGateway.safeToken("X".repeat(80)));
 }
}
