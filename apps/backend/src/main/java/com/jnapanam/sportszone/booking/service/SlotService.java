package com.jnapanam.sportszone.booking.service;

import com.jnapanam.sportszone.booking.dto.SlotResponse;
import com.jnapanam.sportszone.booking.model.BookingSlot;
import com.jnapanam.sportszone.booking.repository.BookingSlotRepository;
import com.jnapanam.sportszone.catalog.model.*;
import com.jnapanam.sportszone.catalog.repository.*;
import com.jnapanam.sportszone.catalog.service.CatalogueService;
import com.jnapanam.sportszone.common.exception.BadRequestException;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SlotService {
 private final CatalogueService catalogue; private final BookingSlotRepository slots; private final FacilityOperatingHourRepository hours;
 private final FacilityClosureRepository closures; private final FacilityScheduleOverrideRepository overrides; private final BookingService bookings;
 public SlotService(CatalogueService catalogue,BookingSlotRepository slots,FacilityOperatingHourRepository hours,FacilityClosureRepository closures,FacilityScheduleOverrideRepository overrides,BookingService bookings){this.catalogue=catalogue;this.slots=slots;this.hours=hours;this.closures=closures;this.overrides=overrides;this.bookings=bookings;}

 @Transactional
 public List<SlotResponse> availability(UUID facilityId, LocalDate date) {
  bookings.cleanupExpired();
  bookings.cleanupUnpaid();
  if(date == null) throw new BadRequestException("date is required");
  if(date.isBefore(LocalDate.now(ZoneId.of("Asia/Kolkata")))) throw new BadRequestException("Past dates are not bookable");
  // Serialize materialization of this facility's slots to avoid duplicate-start races.
  Facility facility = catalogue.requireFacilityLocked(facilityId);
  if(!facility.isAvailableOn(date) || closures.existsByFacilityIdAndClosureDate(facilityId,date))
      return List.of();
  List<BookingSlot> existing = slots.findByFacilityIdAndDateOrderByStartTimeAsc(facilityId,date);
  Map<LocalTime,BookingSlot> byStart = new HashMap<>();
  existing.forEach(s -> byStart.put(s.getStartTime(),s));
  List<BookingSlot> desired = generate(facility,date);
  List<BookingSlot> bookable = new ArrayList<>();
  for(BookingSlot candidate : desired) {
    BookingSlot current = byStart.get(candidate.getStartTime());
    if(current != null) {
      if((!current.getEndTime().equals(candidate.getEndTime()) || current.getCapacity()!=candidate.getCapacity())
           && current.getHeldCount()+current.getBookedCount()>0) continue;
      if(current.getHeldCount()+current.getBookedCount()==0)
          current.reconfigure(candidate.getEndTime(),candidate.getCapacity());
      bookable.add(current);
    } else {
      // Never sell an overlapping new slot while an old reservation/hold is still active.
      if(existing.stream().anyMatch(s -> s.getHeldCount()+s.getBookedCount()>0
              && s.getStartTime().isBefore(candidate.getEndTime())
              && s.getEndTime().plusMinutes(facility.getBufferMinutes()).isAfter(candidate.getStartTime()))) continue;
      bookable.add(candidate);
    }
  }
  slots.saveAll(bookable);
  LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
  return bookable.stream().filter(slot -> date.isAfter(now.toLocalDate()) || slot.getStartTime().isAfter(now.toLocalTime()))
      .sorted(Comparator.comparing(BookingSlot::getStartTime)).map(SlotResponse::from).toList();
 }

 private List<BookingSlot> generate(Facility facility, LocalDate date) {
  Map<LocalTime,BookingSlot> generated = new TreeMap<>();
  for(FacilityOperatingHour hour : hours.findByFacilityIdAndDayOfWeekOrderByOpenTimeAsc(facility.getId(),date.getDayOfWeek()))
    if(!hour.isClosed())
      addRange(generated, facility,date,hour.getOpenTime(),hour.getCloseTime(),facility.getSlotDurationMinutes(),facility.getBufferMinutes(),facility.getCapacity());
  for(FacilityScheduleOverride value : overrides.findByFacilityIdAndScheduleDateOrderByStartTimeAsc(facility.getId(),date)) {
    if(value.getAvailability()==Availability.UNAVAILABLE) {
      generated.entrySet().removeIf(e -> overlaps(e.getValue(),value.getStartTime(),value.getEndTime()));
    } else {
      addRange(generated,facility,date,value.getStartTime(),value.getEndTime(),
          value.getSlotDurationMinutes()==null?facility.getSlotDurationMinutes():value.getSlotDurationMinutes(),
          facility.getBufferMinutes(),value.getCapacity()==null?facility.getCapacity():value.getCapacity());
    }
  }
  return List.copyOf(generated.values());
 }
 private static void addRange(Map<LocalTime,BookingSlot> target, Facility facility,LocalDate date,
                              LocalTime start,LocalTime end,int duration,int buffer,int capacity) {
  if(duration<=0 || buffer<0) throw new BadRequestException("Invalid slot duration or buffer");
  int cursorMinutes=start.toSecondOfDay()/60;
  int endMinutes=end.toSecondOfDay()/60;
  for(;cursorMinutes+duration<=endMinutes;cursorMinutes+=duration+buffer) {
    LocalTime cursor=LocalTime.of(cursorMinutes/60,cursorMinutes%60);
    // A slot's end marks the end of play; its buffer is not customer-bookable.
    target.put(cursor,new BookingSlot(facility,date,cursor,cursor.plusMinutes(duration),capacity));
  }
 }
 private static boolean overlaps(BookingSlot slot,LocalTime start,LocalTime end) {
  return slot.getStartTime().isBefore(end) && slot.getEndTime().isAfter(start);
 }
}
