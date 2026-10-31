package com.jnapanam.sportszone.payment.subscription;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnapanam.sportszone.common.exception.BadRequestException;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class RecurringRefundReconciliationTest {
 private final JdbcTemplate db=mock(JdbcTemplate.class);
 private final RecurringWebhookService service=new RecurringWebhookService(db);
 private final ObjectMapper json=new ObjectMapper();
 private final UUID chargeId=UUID.randomUUID();
 private com.fasterxml.jackson.databind.JsonNode payload(String id,String payment,long amount)throws Exception{
  return json.readTree("""
   {"refund":{"entity":{"id":"%s","payment_id":"%s","amount":%d}}}
   """.formatted(id,payment,amount));
 }
 @Test void unlinkedRefundRemainsUnmatched()throws Exception{
  when(db.queryForList(contains("recurring_charges where"),eq("pay_normal"))).thenReturn(List.of());
  assertEquals("UNMATCHED",service.refund("refund.processed",payload("rfnd_new","pay_normal",5000)));
 }
 @Test void processedRefundIsStoredAgainstCapturedRecurringCharge()throws Exception{
  when(db.queryForList(contains("recurring_charges where"),eq("pay_recurring")))
    .thenReturn(List.of(Map.of("id",chargeId,"amount_paise",30000L)));
  when(db.queryForList(contains("recurring_charge_refunds where"),eq("rfnd_12345")))
    .thenReturn(List.of());
  when(db.queryForObject(contains("coalesce(sum"),eq(Long.class),eq(chargeId))).thenReturn(0L);
  assertEquals("APPLIED",service.refund("refund.processed",payload("rfnd_12345","pay_recurring",10000)));
  verify(db).update(contains("insert into recurring_charge_refunds"),any(UUID.class),eq(chargeId),eq("rfnd_12345"),eq(10000L),eq("PROCESSED"));
 }
 @Test void refundMustNotExceedCapturedCharge()throws Exception{
  when(db.queryForList(contains("recurring_charges where"),eq("pay_recurring")))
    .thenReturn(List.of(Map.of("id",chargeId,"amount_paise",10000L)));
  assertThrows(BadRequestException.class,()->service.refund("refund.processed",payload("rfnd_12345","pay_recurring",20000)));
  verify(db,never()).update(anyString(),any(),any(),any(),any(),any());
 }
 @Test void duplicateProcessedRefundDoesNotAddSecondRecord()throws Exception{
  when(db.queryForList(contains("recurring_charges where"),eq("pay_recurring")))
    .thenReturn(List.of(Map.of("id",chargeId,"amount_paise",30000L)));
  when(db.queryForList(contains("recurring_charge_refunds where"),eq("rfnd_12345")))
    .thenReturn(List.of(Map.of("id",UUID.randomUUID(),"status","PROCESSED","amount_paise",5000L)));
  assertEquals("OBSERVED",service.refund("refund.processed",payload("rfnd_12345","pay_recurring",5000)));
  verify(db,never()).update(anyString(),any(),any());
 }
 @Test void refundCreatedDoesNotCountAsCompleted()throws Exception{
  when(db.queryForList(contains("recurring_charges where"),eq("pay_recurring")))
    .thenReturn(List.of(Map.of("id",chargeId,"amount_paise",30000L)));
  when(db.queryForList(contains("recurring_charge_refunds where"),eq("rfnd_12345")))
    .thenReturn(List.of());
  assertEquals("APPLIED",service.refund("refund.created",payload("rfnd_12345","pay_recurring",5000)));
  verify(db).update(contains("insert into recurring_charge_refunds"),any(UUID.class),eq(chargeId),eq("rfnd_12345"),eq(5000L),eq("CREATED"));
 }
}
