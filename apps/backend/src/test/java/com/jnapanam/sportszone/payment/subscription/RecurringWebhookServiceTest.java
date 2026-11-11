package com.jnapanam.sportszone.payment.subscription;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnapanam.sportszone.common.exception.BadRequestException;
import com.jnapanam.sportszone.common.exception.ConflictException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class RecurringWebhookServiceTest {
 final JdbcTemplate db=mock(JdbcTemplate.class);
 final RecurringWebhookService service=new RecurringWebhookService(db);
 final ObjectMapper mapper=new ObjectMapper();
 final UUID agreement=UUID.randomUUID(), owner=UUID.randomUUID(), membership=UUID.randomUUID();
 Map<String,Object> linked(){
  Map<String,Object> m=new HashMap<>();
  m.put("id",agreement);m.put("kind","MEMBERSHIP");m.put("reference_id",membership);m.put("owner_id",owner);
  m.put("razorpay_plan_id","plan_JNAPANAM");m.put("amount_paise",120000L);m.put("paid_cycles",0);
  m.put("total_cycles",3);m.put("status","AUTHENTICATED");
  return m;
 }
 JsonNode event(String status,String currency,long amount) throws Exception{
   return mapper.readTree("""
   {"subscription":{"entity":{"id":"sub_123456","plan_id":"plan_JNAPANAM"}},
   "payment":{"entity":{"id":"pay_123456","currency":"%s","amount":%d,"status":"%s"}}}
   """.formatted(currency,amount,status));
 }
 @BeforeEach void setup(){
   when(db.queryForList(contains("recurring_agreements a"),eq("sub_123456"))).thenReturn(List.of(linked()));
 }
 @Test void unrecognizedSubscriptionIsObservedOnly() throws Exception {
   assertEquals("OBSERVED",service.accept("subscription.updated",event("captured","INR",120000)));
   verify(db,never()).update(contains("recurring_charges"),any(),any(),any(),any());
 }
 @Test void badPaymentStatusNeverGrantsBenefits() throws Exception {
   assertThrows(BadRequestException.class,()->service.accept("subscription.charged",event("authorized","INR",120000)));
   verify(db,never()).update(contains("customer_memberships"),any(),any(),any());
 }
 @Test void amountMismatchRejectedBeforeEntitlement() throws Exception {
   assertThrows(BadRequestException.class,()->service.accept("subscription.charged",event("captured","INR",100)));
   verify(db,never()).update(contains("recurring_charges"),any(),any(),any(),any());
 }
 @Test void currencyMismatchRejectedBeforeEntitlement() throws Exception {
   assertThrows(BadRequestException.class,()->service.accept("subscription.charged",event("captured","USD",120000)));
 }
 @Test void duplicateCapturedPaymentDoesNotExtendMembershipTwice() throws Exception {
   when(db.queryForObject(contains("recurring_charges"),eq(Integer.class),eq("pay_123456"))).thenReturn(1);
   assertEquals("OBSERVED",service.accept("subscription.charged",event("captured","INR",120000)));
   verify(db,never()).update(contains("customer_memberships"),any(),any(),any());
 }
 @Test void subscriptionHaltedDoesNotChargeOrExtendAccess() throws Exception {
   assertEquals("APPLIED",service.accept("subscription.halted",event("captured","INR",120000)));
   verify(db).update(contains("recurring_agreements set status"),eq("HALTED"),eq(agreement));
   verify(db,never()).update(contains("recurring_charges"),any(),any(),any(),any());
 }
 @Test void firstCapturedChargeActivatesMembership() throws Exception {
   when(db.queryForObject(contains("recurring_charges"),eq(Integer.class),eq("pay_123456"))).thenReturn(0);
   when(db.queryForList(contains("customer_memberships m"),eq(membership),eq(owner)))
      .thenReturn(List.of(Map.of("status","PENDING_PAYMENT","duration_days",30)));
   assertEquals("APPLIED",service.accept("subscription.charged",event("captured","INR",120000)));
   verify(db).update(contains("customer_memberships set status"),any(),any(),eq(membership));
   verify(db).update(contains("recurring_charges"),any(),eq(agreement),eq("pay_123456"),eq(120000L));
   verify(db).update(contains("recurring_agreements set paid_cycles"),eq("ACTIVE"),eq(agreement));
 }
 @Test void cancelledMembershipPreventsAutoRenewal() throws Exception {
   when(db.queryForObject(contains("recurring_charges"),eq(Integer.class),eq("pay_123456"))).thenReturn(0);
   when(db.queryForList(contains("customer_memberships m"),eq(membership),eq(owner)))
     .thenReturn(List.of(Map.of("status","CANCELLED","duration_days",30)));
   assertThrows(ConflictException.class,()->service.accept("subscription.charged",event("captured","INR",120000)));
   verify(db,never()).update(contains("recurring_charges"),any(),any(),any(),any());
 }
}
