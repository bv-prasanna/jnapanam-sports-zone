package com.jnapanam.sportszone.payment.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jnapanam.sportszone.common.exception.ConflictException;
import com.jnapanam.sportszone.payment.config.PaymentProperties;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.springframework.stereotype.Component;

/** Server-only Razorpay Subscriptions API. Never expose secret keys to a browser. */
@Component
public class RazorpaySubscriptionsClient {
 private final PaymentProperties config;
 private final ObjectMapper json;
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(7)).build();
 public RazorpaySubscriptionsClient(PaymentProperties config,ObjectMapper json){this.config=config;this.json=json;}
 public boolean configured(){return "razorpay".equalsIgnoreCase(config.provider())&&config.keyId()!=null
   &&!config.keyId().isBlank()&&config.keySecret()!=null&&!config.keySecret().isBlank();}
 public String key(){return config.keyId();}
 public String sign(String input){
  try{
    var mac=javax.crypto.Mac.getInstance("HmacSHA256");
    mac.init(new javax.crypto.spec.SecretKeySpec(config.keySecret().getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
    return java.util.HexFormat.of().formatHex(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
  }catch(Exception e){throw new IllegalStateException("Unable to verify subscription authorization",e);}
 }
 public String plan(String name,long amount,String period,int interval){
   ObjectNode body=json.createObjectNode().put("period",period).put("interval",interval);
   body.putObject("item").put("name",name).put("amount",amount).put("currency","INR");
   return request("POST","/plans",body).path("id").asText();
 }
 public String subscription(String planId,int cycles,String reference){
   ObjectNode body=json.createObjectNode().put("plan_id",planId).put("total_count",cycles).put("quantity",1).put("customer_notify",false);
   body.putObject("notes").put("jnapanam_ref",reference);
   return request("POST","/subscriptions",body).path("id").asText();
 }
 public void cancel(String subscriptionId){request("POST","/subscriptions/"+subscriptionId+"/cancel",
   json.createObjectNode().put("cancel_at_cycle_end",false));}
 public JsonNode subscriptionDetails(String subscriptionId){return request("GET","/subscriptions/"+subscriptionId,null);}
 private JsonNode request(String method,String endpoint,JsonNode body){
   if(!configured())throw new ConflictException("Razorpay provider must be configured for recurring billing");
   try {
     String authorization=Base64.getEncoder().encodeToString((config.keyId()+":"+config.keySecret()).getBytes(StandardCharsets.UTF_8));
     HttpRequest.Builder builder=HttpRequest.newBuilder(URI.create("https://api.razorpay.com/v1"+endpoint))
         .header("Authorization","Basic "+authorization).timeout(Duration.ofSeconds(18));
     if("GET".equals(method)) builder.GET();
     else builder.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
     var response=http.send(builder.build(),HttpResponse.BodyHandlers.ofString());
     if(response.statusCode()/100!=2)throw new ConflictException("Razorpay subscription request rejected ("+response.statusCode()+"). Verify your Subscriptions product is enabled.");
     return json.readTree(response.body());
   } catch(ConflictException e){throw e;}catch(Exception e){
     Thread.currentThread().interruptIf(e instanceof InterruptedException);
     throw new ConflictException("Razorpay subscriptions unavailable; retry later");
   }
 }
}
