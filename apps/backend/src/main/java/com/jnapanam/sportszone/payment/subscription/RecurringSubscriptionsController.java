package com.jnapanam.sportszone.payment.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class RecurringSubscriptionsController {
 private final RecurringSubscriptionService service;
 private final ObjectMapper json;
 public RecurringSubscriptionsController(RecurringSubscriptionService service,ObjectMapper json){
   this.service=service;this.json=json;
 }
 @GetMapping("/api/v1/admin/subscriptions/offerings")
 @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
 public List<Map<String,Object>> adminOfferings(){return service.offerings(true);}
 @PostMapping("/api/v1/admin/subscriptions/offerings")
 @PreAuthorize("hasRole('SUPER_ADMIN')")
 @ResponseStatus(HttpStatus.CREATED)
 public Map<String,Object> create(@RequestBody RecurringSubscriptionService.OfferingRequest request){
   return service.configure(request);
 }
 public record EnableRequest(boolean enabled){}
 @PutMapping("/api/v1/admin/subscriptions/offerings/{id}/availability")
 @PreAuthorize("hasRole('SUPER_ADMIN')")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void available(@PathVariable UUID id,@RequestBody EnableRequest request){service.setEnabled(id,request.enabled());}
 @GetMapping("/api/v1/admin/subscriptions")
 @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
 public List<Map<String,Object>> all(){return service.all();}
 @GetMapping("/api/v1/subscriptions/offerings")
 public List<Map<String,Object>> publicOfferings(){return service.offerings(false);}
 @GetMapping("/api/v1/subscriptions/me")
 public List<Map<String,Object>> mine(Authentication auth){return service.mine(UUID.fromString(auth.getName()));}
 @PostMapping("/api/v1/subscriptions/start")
 @ResponseStatus(HttpStatus.CREATED)
 public RecurringSubscriptionService.StartResult start(Authentication auth,@RequestBody RecurringSubscriptionService.StartRequest request){
   return service.start(UUID.fromString(auth.getName()),request,"");
 }
 @PostMapping("/api/v1/subscriptions/{id}/cancel")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void cancel(Authentication auth,@PathVariable UUID id){
   service.cancel(UUID.fromString(auth.getName()),id,false);
 }
 @PostMapping("/api/v1/admin/subscriptions/{id}/cancel")
 @PreAuthorize("hasRole('SUPER_ADMIN')")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void cancelAdmin(@PathVariable UUID id){service.cancel(null,id,true);}
 @GetMapping(value="/api/v1/public/subscriptions/checkout/{token}",produces=MediaType.TEXT_HTML_VALUE)
 public ResponseEntity<String> checkout(@PathVariable String token) throws Exception{
   var opts=service.checkout(token);
   String config=json.writeValueAsString(Map.of("key",opts.get("key"),"subscription_id",opts.get("subscriptionId"),
       "name","Jnapanam Sports Zone","description",opts.get("title")));
   config=config.replace("<","\\u003c");
   String html="""
     <!doctype html><html lang="en"><head><meta charset="utf-8">
     <meta name="viewport" content="width=device-width,initial-scale=1">
     <title>Authorize Jnapanam recurring billing</title>
     <style>body{font-family:system-ui,sans-serif;background:#f4f8fb;color:#063b63;max-width:500px;
     margin:8vh auto;padding:25px;line-height:1.6}button{background:#0877b9;color:white;
     padding:14px 20px;border-radius:10px;border:0;font-weight:800}button:disabled{opacity:.5}</style>
     </head><body><h1>Recurring billing authorization</h1>
     <p>Razorpay will request your consent for scheduled charges. You can cancel future charges from Jnapanam.</p>
     <button id="authorize" disabled>Loading Razorpay…</button><p id="status"></p>
     <script src="https://checkout.razorpay.com/v1/checkout.js"></script><script>
     const options=__OPTIONS__;
     const status=document.getElementById("status");
     const button=document.getElementById("authorize");
     if(window.Razorpay){button.disabled=false;button.textContent="Authorize recurring payment";}
     else status.textContent="Razorpay is unavailable. Retry later.";
     button.addEventListener("click",()=>{
       button.disabled=true;
       const checkout=new Razorpay({...options,handler:async (response)=>{
         status.textContent="Verifying mandate authorization…";
         try {
           const result=await fetch(window.location.pathname+"/verify",{
             method:"POST",headers:{"Content-Type":"application/json"},
             body:JSON.stringify({paymentId:response.razorpay_payment_id,
               subscriptionId:response.razorpay_subscription_id,signature:response.razorpay_signature})
           });
           if(!result.ok)throw new Error("Unverified");
           status.textContent="Mandate authorized. Your access begins only after a captured installment is confirmed.";
           button.style.display="none";
         }catch(e){status.textContent="Authorization not confirmed. Please contact support before retrying.";}
       },modal:{ondismiss:()=>{button.disabled=false;status.textContent="Authorization cancelled.";}}});
       checkout.on("payment.failed",()=>{button.disabled=false;status.textContent="Authorization payment failed.";});
       checkout.open();
     });
     </script></body></html>
     """.replace("__OPTIONS__",config);
   return ResponseEntity.ok().cacheControl(CacheControl.noStore())
     .header("Referrer-Policy","no-referrer")
     .header("X-Content-Type-Options","nosniff")
     .header("Content-Security-Policy","default-src 'self'; script-src 'self' 'unsafe-inline' https://checkout.razorpay.com; style-src 'self' 'unsafe-inline'; frame-src https://api.razorpay.com https://*.razorpay.com; connect-src 'self' https://*.razorpay.com; img-src 'self' data: https://*.razorpay.com")
     .body(html);
 }
 public record VerifyRequest(String paymentId,String subscriptionId,String signature){}
 @PostMapping("/api/v1/public/subscriptions/checkout/{token}/verify")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void verify(@PathVariable String token,@RequestBody VerifyRequest req){
   if(req==null)throw new com.jnapanam.sportszone.common.exception.BadRequestException("Missing mandate proof");
   service.verifiedAuthorization(token,req.paymentId(),req.subscriptionId(),req.signature());
 }
}
