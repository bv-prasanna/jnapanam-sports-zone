package com.jnapanam.sportszone.payment.gateway;
public interface PaymentGateway{
 record Order(String id){} record Refund(String id){}
 record CapturedPayment(String orderId,long amountPaise,String currency,boolean captured){
  public boolean matches(String order,long amount,String money){
   return captured && java.util.Objects.equals(orderId,order) && amountPaise==amount
      && java.util.Objects.equals(currency,money);
  }
 }
 Order createOrder(String receipt,long amountPaise,String currency);
 boolean verifyPayment(String orderId,String paymentId,String signature);
 boolean verifyWebhook(String body,String signature);
 CapturedPayment fetchPayment(String paymentId);
 Refund refund(String paymentId,long amountPaise,String reason);
 String provider(); String publicKey();
}
