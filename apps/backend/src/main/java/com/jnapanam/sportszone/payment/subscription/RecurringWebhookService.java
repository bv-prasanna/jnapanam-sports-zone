package com.jnapanam.sportszone.payment.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.jnapanam.sportszone.common.exception.BadRequestException;
import com.jnapanam.sportszone.common.exception.ConflictException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Invoked only from PaymentService after Razorpay HMAC verification and event deduplication. */
@Service
public class RecurringWebhookService {
 private final JdbcTemplate db;
 public RecurringWebhookService(JdbcTemplate db){this.db=db;}
 public String accept(String event,JsonNode payload){
   if(!event.startsWith("subscription."))return "OBSERVED";
   JsonNode sub=payload.path("subscription").path("entity");
   String id=sub.path("id").asText("");
   if(!id.matches("[A-Za-z0-9_-]{5,120}"))throw new BadRequestException("Invalid subscription identifier");
   var agreements=db.queryForList("""
      select a.*,o.amount_paise,o.razorpay_plan_id,o.total_cycles,o.period,
        o.interval_count from recurring_agreements a
      join recurring_offerings o on o.id=a.offering_id
      where a.provider_subscription_id=? for update of a
      """,id);
   if(agreements.isEmpty())return "UNMATCHED";
   var a=agreements.get(0);
   if(!String.valueOf(a.get("razorpay_plan_id")).equals(sub.path("plan_id").asText()))
     throw new BadRequestException("Subscription provider plan mismatch");
   UUID agreementId=(UUID)a.get("id");
   if("subscription.charged".equals(event))return charge(a,payload,agreementId);
   String status=switch(event){
     case "subscription.authenticated"->"AUTHENTICATED";
     case "subscription.activated","subscription.resumed"->"ACTIVE";
     case "subscription.pending"->"PENDING";
     case "subscription.halted"->"HALTED";
     case "subscription.paused"->"PAUSED";
     case "subscription.cancelled"->"CANCELLED";
     case "subscription.completed"->"COMPLETED";
     case "subscription.expired"->"EXPIRED";
     default->null;
   };
   if(status==null)return "OBSERVED";
   String current=(String)a.get("status");
   if(List.of("COMPLETED","CANCELLED","EXPIRED").contains(current)&&!current.equals(status))return "OBSERVED";
   db.update("update recurring_agreements set status=?,updated_at=now() where id=?",status,agreementId);
   return "APPLIED";
 }
 public String refund(String event,JsonNode payload){
   if(!List.of("refund.created","refund.processed","refund.failed").contains(event))return "OBSERVED";
   JsonNode item=payload.path("refund").path("entity");
   String refundId=item.path("id").asText("");
   String paymentId=item.path("payment_id").asText("");
   long amount=item.path("amount").asLong(-1);
   if(!refundId.matches("[A-Za-z0-9_-]{5,120}")||!paymentId.matches("[A-Za-z0-9_-]{5,120}")||amount<=0)
     throw new BadRequestException("Invalid recurring refund identifiers or amount");
   var charges=db.queryForList("select id,amount_paise from recurring_charges where provider_payment_id=? for update",paymentId);
   if(charges.isEmpty())return "UNMATCHED";
   UUID chargeId=(UUID)charges.get(0).get("id");
   long paid=((Number)charges.get(0).get("amount_paise")).longValue();
   if(amount>paid)throw new BadRequestException("Recurring refund exceeds original charge");
   var previous=db.queryForList("select id,status,amount_paise from recurring_charge_refunds where provider_refund_id=?",refundId);
   String status=switch(event){case "refund.processed"->"PROCESSED";case "refund.failed"->"FAILED";default->"CREATED";};
   if(previous.isEmpty()){
     Long refunded="PROCESSED".equals(status)
         ? db.queryForObject("select coalesce(sum(amount_paise),0) from recurring_charge_refunds where charge_id=? and status='PROCESSED'",Long.class,chargeId)
         : 0L;
     if("PROCESSED".equals(status)&&(refunded==null?0:refunded)+amount>paid)
       throw new BadRequestException("Recurring refund aggregate exceeds captured charge");
     db.update("""
       insert into recurring_charge_refunds(id,charge_id,provider_refund_id,amount_paise,status)
       values(?,?,?,?,?)
       """,UUID.randomUUID(),chargeId,refundId,amount,status);
     return "APPLIED";
   }
   var prior=previous.get(0);
   if(((Number)prior.get("amount_paise")).longValue()!=amount)
     throw new BadRequestException("Recurring refund amount mismatch for same provider id");
   if("PROCESSED".equals(prior.get("status"))||status.equals(prior.get("status")))return "OBSERVED";
   if("PROCESSED".equals(status)){
     Long refunded=db.queryForObject("select coalesce(sum(amount_paise),0) from recurring_charge_refunds where charge_id=? and status='PROCESSED'",Long.class,chargeId);
     if((refunded==null?0:refunded)+amount>paid)
       throw new BadRequestException("Recurring refund aggregate exceeds captured charge");
   }
   db.update("update recurring_charge_refunds set status=?,updated_at=now() where id=?",status,prior.get("id"));
   return "APPLIED";
 }
 private String charge(java.util.Map<String,Object>a,JsonNode payload,UUID agreementId){
   var charge=payload.path("payment").path("entity");
   String paymentId=charge.path("id").asText("");
   if(!paymentId.matches("[A-Za-z0-9_-]{5,120}"))
     throw new BadRequestException("Subscription charge must include provider payment id");
   long amount=charge.path("amount").asLong(-1);
   if(amount!=((Number)a.get("amount_paise")).longValue()||
         !"INR".equalsIgnoreCase(charge.path("currency").asText()))
     throw new BadRequestException("Subscription charge amount or currency mismatch");
   if(!"captured".equalsIgnoreCase(charge.path("status").asText("")))
     throw new BadRequestException("Subscription installment has not been captured");
   if(db.queryForObject("select count(*) from recurring_charges where provider_payment_id=?",Integer.class,paymentId)>0)
     return "OBSERVED";
   int cycles=((Number)a.get("paid_cycles")).intValue();
   if(cycles>=((Number)a.get("total_cycles")).intValue())
     throw new ConflictException("Received charge beyond configured recurring cycle count; reconciliation required");
   String kind=(String)a.get("kind");
   UUID ref=(UUID)a.get("reference_id");
   UUID owner=(UUID)a.get("owner_id");
   if("MEMBERSHIP".equals(kind)){
     var membership=db.queryForList("""
       select m.status,m.start_date,m.end_date,p.duration_days
       from customer_memberships m join membership_plans p on p.id=m.plan_id
       where m.id=? and m.user_id=? for update of m
       """,ref,owner);
     if(membership.isEmpty())throw new ConflictException("Recurring membership not found");
     var m=membership.get(0);String state=(String)m.get("status");
     if(!List.of("PENDING_PAYMENT","ACTIVE","EXPIRED").contains(state))
       throw new ConflictException("Cancelled or suspended membership cannot be renewed; resolve with admin");
     int days=((Number)m.get("duration_days")).intValue();
     LocalDate today=LocalDate.now(ZoneId.of("Asia/Kolkata"));
     LocalDate start= m.get("end_date")==null?today:
       ((Date)m.get("end_date")).toLocalDate().plusDays(1);
     if(start.isBefore(today))start=today;
     db.update("""
       update customer_memberships set status='ACTIVE',start_date=coalesce(start_date,?),
       end_date=?,activated_at=coalesce(activated_at,now()),payment_expires_at=null,updated_at=now(),status_reason=null where id=?
       """,Date.valueOf(start),Date.valueOf(start.plusDays(days-1L)),ref);
   }else if("TRAINING".equals(kind)){
     var training=db.queryForList("""
       select e.status,b.end_date from training_enrollments e
       join training_batches b on b.id=e.batch_id
       where e.id=? and e.user_id=? for update of e
       """,ref,owner);
     if(training.isEmpty())throw new ConflictException("Recurring training enrollment not found");
     var t=training.get(0);
     if(((Date)t.get("end_date")).toLocalDate().isBefore(LocalDate.now(ZoneId.of("Asia/Kolkata"))))
       throw new ConflictException("Training ended; manual refund/reconciliation required");
     if(!List.of("PENDING_PAYMENT","ENROLLED").contains(String.valueOf(t.get("status"))))
       throw new ConflictException("Cancelled training cannot be billed; manual reconciliation required");
     db.update("""
       update training_enrollments set status='ENROLLED',
         payment_completed_at=coalesce(payment_completed_at,now()),payment_expires_at=null
       where id=?
       """,ref);
   } else throw new BadRequestException("Unsupported subscription offering");
   db.update("insert into recurring_charges(id,agreement_id,provider_payment_id,amount_paise) values(?,?,?,?)",
        UUID.randomUUID(),agreementId,paymentId,amount);
   String current=(String)a.get("status");
   db.update("""
       update recurring_agreements set paid_cycles=paid_cycles+1,
       last_charge_at=now(),status=?,updated_at=now() where id=?
       """,List.of("CANCELLED","COMPLETED","EXPIRED").contains(current)?current:"ACTIVE",agreementId);
   return "APPLIED";
 }
}
