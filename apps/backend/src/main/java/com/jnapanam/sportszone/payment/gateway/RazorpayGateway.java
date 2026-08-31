package com.jnapanam.sportszone.payment.gateway;
import com.fasterxml.jackson.databind.*;import com.fasterxml.jackson.databind.node.ObjectNode;import com.jnapanam.sportszone.common.exception.BadRequestException;import com.jnapanam.sportszone.payment.config.PaymentProperties;
import java.net.URI;import java.net.http.*;import java.nio.charset.StandardCharsets;import java.time.Duration;import java.util.Base64;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.stereotype.Component;
@Component @ConditionalOnProperty(name="app.payment.provider",havingValue="razorpay")
public class RazorpayGateway implements PaymentGateway{
 private final PaymentProperties p;private final ObjectMapper json;private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
 public RazorpayGateway(PaymentProperties p,ObjectMapper json){this.p=p;this.json=json;if(blank(p.keyId())||blank(p.keySecret())||blank(p.webhookSecret()))throw new IllegalStateException("Razorpay keys and webhook secret are required");}
 public Order createOrder(String receipt,long amount,String currency){ObjectNode n=json.createObjectNode().put("amount",amount).put("currency",currency).put("receipt",receipt);return new Order(call("/orders",n).path("id").asText());}
 public Refund refund(String paymentId,long amount,String reason){ObjectNode n=json.createObjectNode().put("amount",amount);n.putObject("notes").put("reason",reason);return new Refund(call("/payments/"+paymentId+"/refund",n).path("id").asText());}
 public boolean verifyPayment(String o,String payment,String sig){return SignatureSupport.matches(SignatureSupport.hmac(o+"|"+payment,p.keySecret()),sig);}
 public boolean verifyWebhook(String body,String sig){return SignatureSupport.matches(SignatureSupport.hmac(body,p.webhookSecret()),sig);}
 public String provider(){return "RAZORPAY";}public String publicKey(){return p.keyId();}
 private JsonNode call(String path,JsonNode body){try{String basic=Base64.getEncoder().encodeToString((p.keyId()+":"+p.keySecret()).getBytes(StandardCharsets.UTF_8));HttpRequest req=HttpRequest.newBuilder(URI.create("https://api.razorpay.com/v1"+path)).timeout(Duration.ofSeconds(20)).header("Authorization","Basic "+basic).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();HttpResponse<String>res=http.send(req,HttpResponse.BodyHandlers.ofString());if(res.statusCode()/100!=2)throw new BadRequestException("Payment gateway rejected the request");return json.readTree(res.body());}catch(BadRequestException e){throw e;}catch(Exception e){throw new BadRequestException("Payment gateway is unavailable");}}
 private static boolean blank(String v){return v==null||v.isBlank();}
}
