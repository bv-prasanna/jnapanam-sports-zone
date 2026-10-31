package com.jnapanam.sportszone.payment.subscription;

import com.jnapanam.sportszone.common.exception.BadRequestException;
import com.jnapanam.sportszone.common.exception.ConflictException;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecurringSubscriptionService {
 private final JdbcTemplate db;
 private final RazorpaySubscriptionsClient gateway;
 private final boolean enabled;
 private final SecureRandom random=new SecureRandom();
 public RecurringSubscriptionService(JdbcTemplate db,RazorpaySubscriptionsClient gateway,
      @Value("${app.payment.subscriptions-enabled:false}")boolean enabled){
   this.db=db;this.gateway=gateway;this.enabled=enabled;
 }
 public java.util.Map<String,Boolean> configuration(){return java.util.Map.of("enabled",enabled,"providerConfigured",gateway.configured());}
 public record OfferingRequest(String kind,UUID referenceId,String title,long amountPaise,String period,int interval,int totalCycles){}
 public record StartRequest(UUID offeringId,UUID referenceId,boolean consent){}
 public record StartResult(UUID id,String checkoutUrl,String status){}
 private static final String LIST_OFFERINGS="""
   select o.id,o.kind,o.reference_id as "referenceId",o.title,o.amount_paise as "amountPaise",
          o.period,o.interval_count as "interval",o.total_cycles as "totalCycles",
          o.enabled,o.razorpay_plan_id as "razorpayPlanId"
   from recurring_offerings o
   """;
 @Transactional(readOnly=true)
 public List<Map<String,Object>> offerings(boolean admin){
   return db.queryForList(LIST_OFFERINGS+(admin?"":" where enabled=true")+" order by title");
 }
 @Transactional
 public Map<String,Object> configure(OfferingRequest r){
   if(r==null||r.referenceId()==null||r.title()==null||r.title().isBlank()||r.title().length()>150
        ||r.amountPaise()<100||r.amountPaise()>100_000_000||r.totalCycles()<2||r.totalCycles()>60
        ||r.interval()<1||r.interval()>12||!List.of("weekly","monthly","yearly").contains(r.period())
        ||!List.of("MEMBERSHIP","TRAINING").contains(r.kind()))
     throw new BadRequestException("Invalid recurring offering. Use a priced plan, billing period and 2–60 cycles.");
   if(!gateway.configured())throw new ConflictException("Configure Razorpay API test keys first.");
   String reference=validateOffering(r);
   if(db.queryForObject("select count(*) from recurring_offerings where kind=? and reference_id=?",Integer.class,r.kind(),r.referenceId())>0)
     throw new ConflictException("Recurring offering already exists for this membership plan or training batch");
   String providerPlan=gateway.plan(r.title(),r.amountPaise(),r.period(),r.interval());
   UUID id=UUID.randomUUID();
   db.update("""
     insert into recurring_offerings(id,kind,reference_id,title,amount_paise,period,interval_count,total_cycles,razorpay_plan_id,enabled)
     values(?,?,?,?,?,?,?,?,?,false)
     """,id,r.kind(),r.referenceId(),r.title(),r.amountPaise(),r.period(),r.interval(),r.totalCycles(),providerPlan);
   return db.queryForMap(LIST_OFFERINGS+" where o.id=?",id);
 }
 private String validateOffering(OfferingRequest r){
   if(r.kind().equals("MEMBERSHIP")){
     var plans=db.queryForList("select code,name,duration_days,status from membership_plans where id=?",r.referenceId());
     if(plans.isEmpty())throw new NotFoundException("Membership plan not found");
     var p=plans.get(0);
     if(!"ACTIVE".equals(p.get("status")))throw new ConflictException("Membership plan must be active");
     int days=((Number)p.get("duration_days")).intValue();
     int required=switch(r.period()){case "weekly"->7*r.interval();case "monthly"->30*r.interval();default->365*r.interval();};
     if(Math.abs(days-required)>r.interval()*3)
       throw new BadRequestException("Membership plan duration must approximately match the recurring billing interval");
     return String.valueOf(p.get("code"));
   }
   var batches=db.queryForList("select name,start_date,end_date,status from training_batches where id=?",r.referenceId());
   if(batches.isEmpty())throw new NotFoundException("Training batch not found");
   var b=batches.get(0);
   if(!"ACTIVE".equals(b.get("status")))throw new ConflictException("Training batch must be active");
   LocalDate start=((java.sql.Date)b.get("start_date")).toLocalDate();
   LocalDate end=((java.sql.Date)b.get("end_date")).toLocalDate();
   LocalDate current=LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
   if(!end.isAfter(current))throw new ConflictException("Training batch has ended");
   long days=java.time.temporal.ChronoUnit.DAYS.between(start.isAfter(current)?start:current,end)+1;
   long minimumCycleDays=switch(r.period()){case "weekly"->7L;case "monthly"->28L;default->365L;};
   if(r.totalCycles()*r.interval()*minimumCycleDays>days+1)
     throw new BadRequestException("Number of billing cycles exceeds the remaining training period");
   return String.valueOf(b.get("name"));
 }
 @Transactional
 public void setEnabled(UUID id,boolean active){
   if(db.update("update recurring_offerings set enabled=? where id=?",active,id)!=1)
      throw new NotFoundException("Recurring offering not found");
 }
 @Transactional(readOnly=true)
 public List<Map<String,Object>> mine(UUID owner){
   return db.queryForList("""
     select a.id,a.kind,a.reference_id as "referenceId",a.provider_subscription_id as "subscriptionId",
       a.status,a.paid_cycles as "paidCycles",a.created_at as "createdAt",
       o.title,o.amount_paise as "amountPaise",o.period,o.total_cycles as "totalCycles"
     from recurring_agreements a join recurring_offerings o on o.id=a.offering_id
     where a.owner_id=? order by a.created_at desc
     """,owner);
 }
 @Transactional
 public StartResult start(UUID owner,StartRequest r,String origin){
   if(!enabled)throw new ConflictException("Recurring billing is disabled until Razorpay subscriptions UAT is complete");
   if(!gateway.configured())throw new ConflictException("Razorpay test credentials not configured");
   if(r==null||!r.consent()||r.offeringId()==null||r.referenceId()==null)
     throw new BadRequestException("Explicit recurring billing consent, plan and enrollment are required");
   var offerings=db.queryForList("select * from recurring_offerings where id=? and enabled=true",r.offeringId());
   if(offerings.isEmpty())throw new NotFoundException("Recurring offering not available");
   var p=offerings.get(0);
   String kind=String.valueOf(p.get("kind"));
   UUID target=((UUID)p.get("reference_id"));
   String lock=kind.equals("MEMBERSHIP")?
       "select m.id from customer_memberships m where m.id=? and m.user_id=? and m.plan_id=? and m.status='PENDING_PAYMENT' for update":
       "select e.id from training_enrollments e where e.id=? and e.user_id=? and e.batch_id=? and e.status='PENDING_PAYMENT' for update";
   if(db.queryForList(lock,r.referenceId(),owner,target).isEmpty())
     throw new ConflictException("Create a pending membership or training enrollment for this exact plan before starting recurring billing.");
   if(db.queryForObject("select count(*) from payment_orders where purpose=? and reference_id=? and status in ('CREATED','PAID','PARTIALLY_REFUNDED')",Integer.class,kind,r.referenceId())>0)
     throw new ConflictException("A one-time payment exists. Select only one billing method.");
   if(!db.queryForList("select id from recurring_agreements where kind=? and reference_id=?",kind,r.referenceId()).isEmpty())
     throw new ConflictException("This membership or enrollment already has a recurring agreement");
   byte[] bytes=new byte[32];random.nextBytes(bytes);
   String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
   String subscription=gateway.subscription(String.valueOf(p.get("razorpay_plan_id")),
       ((Number)p.get("total_cycles")).intValue(),r.referenceId().toString());
   UUID id=UUID.randomUUID();
   var expiry=Instant.now().plusSeconds(24*60*60);
   db.update("""
     insert into recurring_agreements(id,offering_id,owner_id,reference_id,kind,provider_subscription_id,
        authorization_token_hash,authorization_expires_at)
     values(?,?,?,?,?,?,?,?)
     """,id,r.offeringId(),owner,r.referenceId(),kind,subscription,hash(token),java.sql.Timestamp.from(expiry));
   String table=kind.equals("MEMBERSHIP")?"customer_memberships":"training_enrollments";
   db.update("update "+table+" set payment_expires_at=? where id=?",java.sql.Timestamp.from(expiry),r.referenceId());
   return new StartResult(id,origin.replaceAll("/+$","")+"/api/v1/public/subscriptions/checkout/"+token,"CREATED");
 }
 @Transactional(readOnly=true)
 public Map<String,Object> checkout(String token){
   var a=tokenAgreement(token);
   return Map.of("key",gateway.key(),"subscriptionId",a.get("provider_subscription_id"),"title",a.get("title"),
         "amountPaise",a.get("amount_paise"),"period",a.get("period"));
 }
 @Transactional
 public void verifiedAuthorization(String token,String paymentId,String suppliedSubscriptionId,String signature){
   var a=tokenAgreement(token);
   String providerId=String.valueOf(a.get("provider_subscription_id"));
   if(!providerId.equals(suppliedSubscriptionId)||paymentId==null||!paymentId.matches("[A-Za-z0-9_-]{5,120}")
       ||signature==null||!secureEquals(hmac(paymentId+"|"+providerId),signature))
      throw new BadRequestException("Invalid Razorpay recurring authorization signature");
   db.update("""
      update recurring_agreements set status='AUTHENTICATED',updated_at=now()
      where id=? and status='CREATED'
      """,a.get("id"));
   // A mandate authentication is not proof of a captured recurring installment.
 }
 @Transactional
 public void cancel(UUID owner,UUID id,boolean admin){
   var rows=db.queryForList("select * from recurring_agreements where id=?"+(admin?"":" and owner_id=?"),
       admin?new Object[]{id}:new Object[]{id,owner});
   if(rows.isEmpty())throw new NotFoundException("Subscription not found");
   var a=rows.get(0);
   String status=String.valueOf(a.get("status"));
   if(List.of("CANCELLED","COMPLETED","EXPIRED").contains(status))return;
   gateway.cancel(String.valueOf(a.get("provider_subscription_id")));
   db.update("update recurring_agreements set status='CANCELLED',updated_at=now() where id=?",id);
   // Existing paid access remains in effect until its already-paid end date.
 }
 @Transactional
 public void cancelByReference(UUID owner,String kind,UUID ref){
   var rows=db.queryForList("select id from recurring_agreements where owner_id=? and kind=? and reference_id=? and status not in ('CANCELLED','COMPLETED','EXPIRED')",owner,kind,ref);
   for(var row:rows)cancel(owner,(UUID)row.get("id"),false);
 }
 @Transactional(readOnly=true)
 public List<Map<String,Object>> charges(UUID owner,boolean admin){
   String sql="""
     select c.id,c.provider_payment_id as "providerPaymentId",
       c.amount_paise as "amountPaise",c.charged_at as "chargedAt",
       a.kind,o.title,u.full_name as "customerName",a.owner_id as "ownerId"
     from recurring_charges c join recurring_agreements a on a.id=c.agreement_id
     join recurring_offerings o on o.id=a.offering_id
     join users u on u.id=a.owner_id
     """+(admin?"":"where a.owner_id=? ")+"order by c.charged_at desc limit 500";
   return admin?db.queryForList(sql):db.queryForList(sql,owner);
 }
 @Transactional
 public void cancelForTrainingBatch(UUID batchId){
   var rows=db.queryForList("""
       select a.id from recurring_agreements a join training_enrollments e on e.id=a.reference_id
       where a.kind='TRAINING' and e.batch_id=? and a.status not in ('CANCELLED','COMPLETED','EXPIRED')
       """,batchId);
   for(var row:rows)cancel(null,(UUID)row.get("id"),true);
 }
 @Transactional(readOnly=true)
 public List<Map<String,Object>> all(){
   return db.queryForList("""
      select a.id,a.owner_id as "ownerId",u.full_name as "ownerName",u.email as "email",
        a.kind,a.reference_id as "referenceId",a.provider_subscription_id as "subscriptionId",
        a.status,a.paid_cycles as "paidCycles",o.title,o.amount_paise as "amountPaise",o.period
      from recurring_agreements a join recurring_offerings o on o.id=a.offering_id
      join users u on u.id=a.owner_id order by a.created_at desc limit 500
      """);
 }
 private Map<String,Object> tokenAgreement(String token){
   if(token==null||!token.matches("[A-Za-z0-9_-]{43}"))throw new NotFoundException("Authorization expired");
   var rows=db.queryForList("""
     select a.*,o.title,o.amount_paise,o.period
     from recurring_agreements a join recurring_offerings o on o.id=a.offering_id
     where a.authorization_token_hash=? and a.authorization_expires_at>now()
     """,hash(token));
   if(rows.isEmpty())throw new NotFoundException("Authorization link expired");
   return rows.get(0);
 }
 private String hmac(String input){
   // razorpay key secret is held only by the server-side client.
   return gateway.sign(input);
 }
 private static boolean secureEquals(String expected,String actual){
   return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),actual.getBytes(StandardCharsets.US_ASCII));
 }
 private static String hash(String text){
   try{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}
   catch(Exception e){throw new IllegalStateException(e);}
 }
}
