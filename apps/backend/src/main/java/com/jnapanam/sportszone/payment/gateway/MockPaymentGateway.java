package com.jnapanam.sportszone.payment.gateway;
import java.util.UUID;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.stereotype.Component;
@Component @ConditionalOnProperty(name="app.payment.provider",havingValue="mock",matchIfMissing=true)
public class MockPaymentGateway implements PaymentGateway{
 public Order createOrder(String r,long a,String c){return new Order("order_mock_"+UUID.randomUUID().toString().replace("-","").substring(0,16));}
 public boolean verifyPayment(String o,String p,String s){return "mock-signature".equals(s);}
 public boolean verifyWebhook(String b,String s){return "mock-webhook-signature".equals(s);}
 public Refund refund(String p,long a,String r){return new Refund("rfnd_mock_"+UUID.randomUUID().toString().replace("-","").substring(0,16));}
 public String provider(){return "MOCK";}public String publicKey(){return "rzp_test_mock";}
}
