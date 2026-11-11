package com.jnapanam.sportszone.payment.gateway;
/** Provider might have received a refund request although its response could not be confirmed. */
public class RefundGatewayUncertainException extends RuntimeException {
    public RefundGatewayUncertainException(){
        super("Refund submission could not be confirmed. Check Razorpay refunds and reconcile this payment before trying again.");
    }
}
