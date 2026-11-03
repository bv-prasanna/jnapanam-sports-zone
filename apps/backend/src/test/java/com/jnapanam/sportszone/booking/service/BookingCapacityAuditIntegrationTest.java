package com.jnapanam.sportszone.booking.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

/** A public slot can show 19/20 even with no confirmed payments: an active
 * hold or pending booking is enough. Audit must not change those counters.
 */
@SpringBootTest
@ActiveProfiles("test")
class BookingCapacityAuditIntegrationTest {
 @Autowired JdbcTemplate jdbc;
 @Autowired BookingCapacityAuditService audit;

 @Test void showsUnavailableSeatAndFlagsMissingHoldRecordWithoutModifyingCapacity(){
  UUID facility=UUID.randomUUID(),slot=UUID.randomUUID();
  LocalDate day=LocalDate.of(2047,8,16);
  jdbc.update("""
    INSERT INTO facilities(id,sport_id,code,name,slot_duration_minutes,capacity,booking_price,status,created_at,updated_at)
    VALUES(?,? ,?,'Swimming Audit Pool',60,20,150.00,'ACTIVE',now(),now())
    """,facility,UUID.fromString("10000000-0000-0000-0000-000000000001"),
       "AUDIT_"+facility.toString().substring(0,8));
  jdbc.update("""
    INSERT INTO booking_slots(id,facility_id,slot_date,start_time,end_time,capacity,held_count,booked_count,version,created_at)
    VALUES(?,?,?,?,?,20,1,0,0,now())
    """,slot,facility,day,LocalTime.of(6,0),LocalTime.of(6,55));
  try {
    var rows=audit.inspect(facility,day);
    assertEquals(1,rows.size());
    var row=rows.get(0);
    assertEquals("Swimming Audit Pool",row.facilityName());
    assertEquals("Swimming",row.sportName());
    assertEquals(20,row.capacity());
    assertEquals(1,row.heldCount());
    assertEquals(19,row.available());
    assertEquals(0,row.activeHolds());
    assertEquals(0,row.activeBookings());
    assertTrue(row.countersMismatch(),"Persisted hold without an active hold row must be diagnosed");
    assertEquals(1,jdbc.queryForObject("select held_count from booking_slots where id=?",Integer.class,slot));
  } finally {
    jdbc.update("DELETE FROM booking_slots WHERE id=?",slot);
    jdbc.update("DELETE FROM facilities WHERE id=?",facility);
  }
 }
 @Test void doesNotInventMissingSlots(){
  assertTrue(audit.inspect(UUID.randomUUID(),LocalDate.of(2047,8,16)).isEmpty());
 }
}
