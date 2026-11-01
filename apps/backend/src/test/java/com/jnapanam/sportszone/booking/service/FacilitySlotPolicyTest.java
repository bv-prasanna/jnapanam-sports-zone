package com.jnapanam.sportszone.booking.service;

import com.jnapanam.sportszone.booking.model.BookingSlot;
import com.jnapanam.sportszone.booking.repository.BookingSlotRepository;
import com.jnapanam.sportszone.catalog.model.Facility;
import com.jnapanam.sportszone.catalog.model.FacilityOperatingHour;
import com.jnapanam.sportszone.catalog.repository.*;
import com.jnapanam.sportszone.catalog.service.CatalogueService;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FacilitySlotPolicyTest {
 @Test void twoDailyWindowsAndBufferCreateTenFiftyFiveMinuteSlotsWithTwentyCapacity() {
  var catalogue=mock(CatalogueService.class);
  var slots=mock(BookingSlotRepository.class);
  var hours=mock(FacilityOperatingHourRepository.class);
  var closures=mock(FacilityClosureRepository.class);
  var overrides=mock(FacilityScheduleOverrideRepository.class);
  var bookings=mock(BookingService.class);
  UUID id=UUID.randomUUID();LocalDate date=LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(7);
  Facility facility=mock(Facility.class);
  when(facility.getId()).thenReturn(id);when(facility.getCode()).thenReturn("POOL");
  when(facility.isAvailableOn(date)).thenReturn(true);
  when(facility.getSlotDurationMinutes()).thenReturn(55);
  when(facility.getBufferMinutes()).thenReturn(5);when(facility.getCapacity()).thenReturn(20);
  when(catalogue.requireFacilityLocked(id)).thenReturn(facility);
  when(slots.findByFacilityIdAndDateOrderByStartTimeAsc(id,date)).thenReturn(List.of());
  when(overrides.findByFacilityIdAndScheduleDateOrderByStartTimeAsc(id,date)).thenReturn(List.of());
  var first=mock(FacilityOperatingHour.class);var second=mock(FacilityOperatingHour.class);
  when(first.getOpenTime()).thenReturn(LocalTime.of(9,0));
  when(first.getCloseTime()).thenReturn(LocalTime.of(17,0));
  when(second.getOpenTime()).thenReturn(LocalTime.of(19,0));
  when(second.getCloseTime()).thenReturn(LocalTime.of(21,0));
  when(hours.findByFacilityIdAndDayOfWeekOrderByOpenTimeAsc(id,date.getDayOfWeek())).thenReturn(List.of(first,second));
  var result=new SlotService(catalogue,slots,hours,closures,overrides,bookings).availability(id,date);
  assertEquals(10,result.size());
  assertEquals(LocalTime.of(9,0),result.get(0).startTime());
  assertEquals(LocalTime.of(9,55),result.get(0).endTime());
  assertEquals(LocalTime.of(16,0),result.get(7).startTime());
  assertEquals(LocalTime.of(19,0),result.get(8).startTime());
  assertEquals(LocalTime.of(20,55),result.get(9).endTime());
  assertTrue(result.stream().allMatch(s->s.capacity()==20&&s.available()==20));
 }
 @Test void existingBookingPreventsUnsafeCapacityOrDurationChange(){
  Facility facility=mock(Facility.class);
  BookingSlot slot=new BookingSlot(facility,LocalDate.now().plusDays(3),LocalTime.of(9,0),LocalTime.of(10,0),20);
  slot.hold(3);slot.confirm(3);
  assertEquals(17,slot.available());
  assertThrows(com.jnapanam.sportszone.common.exception.ConflictException.class,
   ()->slot.reconfigure(LocalTime.of(9,55),10));
 }
}
