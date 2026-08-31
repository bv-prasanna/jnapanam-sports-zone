package com.jnapanam.sportszone.payment.gateway;
public interface PaymentGateway{
 record Order(String id){} record Refund(String id){}
 Order createOrder(String receipt,long amountPaise,String currency);
 boolean verifyPayment(String orderId,String paymentId,String signature);
 boolean verifyWebhook(String body,String signature);
 Refund refund(String paymentId,long amountPaise,String reason);
 String provider(); String publicKey();
}
