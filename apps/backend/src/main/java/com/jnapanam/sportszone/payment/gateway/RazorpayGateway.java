package com.jnapanam.sportszone.payment.gateway;
import com.fasterxml.jackson.databind.*;import com.fasterxml.jackson.databind.node.ObjectNode;import com.jnapanam.sportszone.common.exception.BadRequestException;import com.jnapanam.sportszone.payment.config.PaymentProperties;
import java.net.URI;import java.net.http.*;import java.nio.charset.StandardCharsets;import java.time.Duration;import java.util.Base64;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.stereotype.Component;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
@Component @ConditionalOnProperty(name="app.payment.provider",havingValue="razorpay")
public class RazorpayGateway implements PaymentGateway{
 private static final Logger LOG=LoggerFactory.getLogger(RazorpayGateway.class);
 private final PaymentProperties p;private final ObjectMapper json;private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
 public RazorpayGateway(PaymentProperties p,ObjectMapper json){this.p=p;this.json=json;if(blank(p.keyId())||blank(p.keySecret())||blank(p.webhookSecret()))throw new IllegalStateException("Razorpay keys and webhook secret are required");}
 public Order createOrder(String receipt,long amount,String currency){
  validateOrderAmount(amount);
  ObjectNode n=json.createObjectNode().put("amount",amount).put("currency",currency).put("receipt",receipt);
  String id=call("/orders",n).path("id").asText();
  if(!id.matches("order_[A-Za-z0-9]+"))throw new BadRequestException("Payment gateway returned an invalid order");
  return new Order(id);
 }
 static void validateOrderAmount(long amount){
  if(amount<100)throw new BadRequestException("Online payments must be at least ₹1 after discounts and loyalty points.");
 }
 public Refund refund(String paymentId,long amount,String reason){
  return refund(paymentId,amount,reason,java.util.UUID.randomUUID().toString());
 }
 public Refund refund(String paymentId,long amount,String reason,String requestId){
  if(paymentId==null||!paymentId.matches("pay_[A-Za-z0-9]+"))
   throw new BadRequestException("The stored Razorpay payment ID is invalid. Reconcile this payment.");
  if(amount<100)throw new BadRequestException("Razorpay refund must be at least ₹1.");
  if(requestId==null||!requestId.matches("[A-Za-z0-9_-]{10,80}"))
   throw new BadRequestException("Refund request reference is invalid.");
  ObjectNode n=json.createObjectNode().put("amount",amount).put("speed","normal");
  n.putObject("notes").put("reason",reason);
  JsonNode data=call("/payments/"+paymentId+"/refund",n,requestId);
  String id=data.path("id").asText(null);
  if(id==null||!id.matches("rfnd_[A-Za-z0-9]+"))throw new RefundGatewayUncertainException();
  return new Refund(id);
 }
 public boolean verifyPayment(String o,String payment,String sig){return SignatureSupport.matches(SignatureSupport.hmac(o+"|"+payment,p.keySecret()),sig);}
 public boolean verifyWebhook(String body,String sig){return SignatureSupport.matches(SignatureSupport.hmac(body,p.webhookSecret()),sig);}
 /** Checkout signature alone is not evidence of a captured payment. */
 public CapturedPayment fetchPayment(String id){
  if(id==null || !id.matches("pay_[A-Za-z0-9]+"))throw new BadRequestException("Invalid payment id");
  try {
   String basic=Base64.getEncoder().encodeToString((p.keyId()+":"+p.keySecret()).getBytes(StandardCharsets.UTF_8));
   HttpRequest req=HttpRequest.newBuilder(URI.create("https://api.razorpay.com/v1/payments/"+id))
     .timeout(Duration.ofSeconds(20)).header("Authorization","Basic "+basic).GET().build();
   HttpResponse<String> response=http.send(req,HttpResponse.BodyHandlers.ofString());
   if(response.statusCode()/100!=2)throw new BadRequestException("Payment verification is unavailable");
   JsonNode payment=json.readTree(response.body());
   return new CapturedPayment(payment.path("order_id").asText(null),
     payment.path("amount").asLong(-1),payment.path("currency").asText(null),
     "captured".equals(payment.path("status").asText()) && payment.path("captured").asBoolean(false));
  }catch(BadRequestException e){throw e;}
   catch(Exception e){throw new BadRequestException("Unable to verify payment capture; refresh before retrying");}
 }
 public String provider(){return "RAZORPAY";}public String publicKey(){return p.keyId();}
 private JsonNode call(String path,JsonNode body){return call(path,body,null);}
 private JsonNode call(String path,JsonNode body,String refundRequestId){
  boolean isRefund=path.startsWith("/payments/")&&path.endsWith("/refund");
  try {
   String basic=Base64.getEncoder().encodeToString((p.keyId()+":"+p.keySecret()).getBytes(StandardCharsets.UTF_8));
   HttpRequest.Builder request=HttpRequest.newBuilder(URI.create("https://api.razorpay.com/v1"+path))
     .timeout(Duration.ofSeconds(20)).header("Authorization","Basic "+basic)
     .header("Content-Type","application/json");
   if(refundRequestId!=null)request.header("X-Refund-Idempotency",refundRequestId);
   HttpResponse<String>res=http.send(request.POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
   if(res.statusCode()/100!=2){
   JsonNode error=json.readTree(res.body()).path("error");
   // Only log bounded, syntactically constrained provider code/field; never log
   // API credentials, authorization headers, payment details or raw responses.
   String operation=path.equals("/orders")?"create_order":"refund";
   LOG.warn("Razorpay {} rejected: httpStatus={} code={} field={} reason={}",
      operation,res.statusCode(),safeToken(error.path("code").asText()),
      safeToken(error.path("field").asText()),safeToken(error.path("reason").asText()));
   if(isRefund && res.statusCode()>=500)throw new RefundGatewayUncertainException();
   throw new BadRequestException(isRefund?refundRejectionMessage(res.statusCode(),safeToken(error.path("reason").asText())):rejectionMessage(res.statusCode()));
  }
  return json.readTree(res.body());
  }catch(BadRequestException|RefundGatewayUncertainException e){throw e;}
   catch(Exception e){
    if(e instanceof InterruptedException)Thread.currentThread().interrupt();
    if(isRefund)throw new RefundGatewayUncertainException();
    throw new BadRequestException("Payment gateway is unavailable");
   }
 }
 static String refundRejectionMessage(int status,String reason){
  if(status==401)return "Razorpay rejected the refund credentials. Contact support; the refund was not submitted.";
  if(status==403)return "Refunds are not enabled for this merchant account. Check Razorpay permissions and merchant balance.";
  if(status==429||status==409)return "Razorpay is processing another request. Check refunds in the Razorpay dashboard before retrying.";
  if(status==400||status==422)return "Razorpay rejected the refund. Check that the payment was captured, the amount remains refundable, and the merchant refund balance is sufficient.";
  return "Razorpay could not confirm the refund. Check its status in Razorpay before retrying.";
 }
 static String rejectionMessage(int status){
  if(status==401)return "Online payments are temporarily unavailable: the venue's payment credentials were rejected. Please contact Jnapanam support.";
  if(status==403)return "Online payments are not enabled for this merchant account. Please contact Jnapanam support.";
  if(status==429)return "The payment service is busy. Please check your existing booking before retrying.";
  if(status==400||status==422)return "Razorpay could not create the payment order. Please contact Jnapanam support with your booking number.";
  return "Razorpay could not process this request. Please check your booking and contact Jnapanam support before retrying.";
 }
 static String safeToken(String value){return value!=null&&value.matches("[A-Za-z0-9_.-]{1,64}")?value:"unavailable";}
 private static boolean blank(String v){return v==null||v.isBlank();}
}
