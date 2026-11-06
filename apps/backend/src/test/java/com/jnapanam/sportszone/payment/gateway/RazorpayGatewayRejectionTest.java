package com.jnapanam.sportszone.payment.gateway;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RazorpayGatewayRejectionTest {
 @Test void rejectedPaymentReturnsSafeActionableText() {
  assertTrue(RazorpayGateway.rejectionMessage(401).contains("credentials"));
  assertTrue(RazorpayGateway.rejectionMessage(403).contains("merchant account"));
  assertTrue(RazorpayGateway.rejectionMessage(400).contains("booking number"));
  assertFalse(RazorpayGateway.rejectionMessage(500).contains("Authorization"));
  assertTrue(RazorpayGateway.refundRejectionMessage(400, "unavailable").contains("refund"));
  assertFalse(RazorpayGateway.refundRejectionMessage(400, "unavailable").contains("create the payment order"));
  assertTrue(RazorpayGateway.refundRejectionMessage(403, "unavailable").contains("merchant account"));
  assertEquals("INSUFFICIENT_BALANCE",RazorpayGateway.refundErrorCategory(null,
    "Your account does not have enough balance to carry out the refund operation."));
  assertTrue(RazorpayGateway.refundRejectionMessage(400,null,
    "Your account does not have enough balance to carry out the refund operation.").contains("balance is insufficient"));
  assertTrue(RazorpayGateway.refundRejectionMessage(400,null,
    "The payment status should be captured for action to be taken.").contains("not captured"));
  assertTrue(RazorpayGateway.refundRejectionMessage(400,null,
    "Refunds cannot be created on your account.").contains("disabled refunds"));
  assertTrue(RazorpayGateway.refundRejectionMessage(400,null,
    "Partial refund is currently not supported for this payment method.").contains("partial refund"));
  assertTrue(RazorpayGateway.refundRejectionMessage(400,null,
    "The refund amount provided is greater than amount captured.").contains("remaining captured amount"));
  assertTrue(RazorpayGateway.refundRejectionMessage(400,null,
    "The payment has been fully refunded already.").contains("already refunded"));
  String sensitive="payment_id pay_sensitive and secret xyz";
  assertEquals("UNCLASSIFIED",RazorpayGateway.refundErrorCategory(null,sensitive));
  assertFalse(RazorpayGateway.refundRejectionMessage(400,null,sensitive).contains("pay_sensitive"));
 }
 @Test void razorpayDiagnosticTokenMustNotLogUntrustedDescriptionOrCredentials(){
  assertEquals("BAD_REQUEST_ERROR",RazorpayGateway.safeToken("BAD_REQUEST_ERROR"));
  assertEquals("unavailable",RazorpayGateway.safeToken("A sensitive error with spaces or secrets"));
  assertEquals("unavailable",RazorpayGateway.safeToken("X".repeat(80)));
 }
}
