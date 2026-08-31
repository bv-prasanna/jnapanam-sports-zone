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
 public List<SlotResponse> availability(UUID facilityId,LocalDate date){
  bookings.cleanupExpired();
  if(date==null) throw new BadRequestException("date is required");
  if(date.isBefore(LocalDate.now())) throw new BadRequestException("Past dates are not bookable");
  Facility facility=catalogue.requireFacility(facilityId);
  if(!facility.isAvailableOn(date)) return List.of();
  List<BookingSlot> existing=slots.findByFacilityIdAndDateOrderByStartTimeAsc(facilityId,date);
  if(existing.isEmpty()) existing=generate(facility,date);
  return existing.stream().map(SlotResponse::from).toList();
 }

 private List<BookingSlot> generate(Facility facility,LocalDate date){
  if(closures.existsByFacilityIdAndClosureDate(facility.getId(),date)) return List.of();
  Map<LocalTime,BookingSlot> generated=new TreeMap<>();
  hours.findByFacilityIdAndDayOfWeek(facility.getId(),date.getDayOfWeek()).filter(h->!h.isClosed()).ifPresent(h->
    addRange(generated,facility,date,h.getOpenTime(),h.getCloseTime(),facility.getSlotDurationMinutes(),facility.getCapacity()));
  List<FacilityScheduleOverride> dayOverrides=overrides.findByFacilityIdAndScheduleDateOrderByStartTimeAsc(facility.getId(),date);
  for(var value:dayOverrides){
   if(value.getAvailability()==Availability.UNAVAILABLE){generated.entrySet().removeIf(e->overlaps(e.getValue(),value.getStartTime(),value.getEndTime()));}
   else addRange(generated,facility,date,value.getStartTime(),value.getEndTime(),value.getSlotDurationMinutes()==null?facility.getSlotDurationMinutes():value.getSlotDurationMinutes(),value.getCapacity()==null?facility.getCapacity():value.getCapacity());
  }
  return slots.saveAll(generated.values()).stream().sorted(Comparator.comparing(BookingSlot::getStartTime)).toList();
 }
 private static void addRange(Map<LocalTime,BookingSlot> target,Facility facility,LocalDate date,LocalTime start,LocalTime end,int duration,int capacity){for(LocalTime cursor=start;!cursor.plusMinutes(duration).isAfter(end);cursor=cursor.plusMinutes(duration))target.put(cursor,new BookingSlot(facility,date,cursor,cursor.plusMinutes(duration),capacity));}
 private static boolean overlaps(BookingSlot slot,LocalTime start,LocalTime end){return slot.getStartTime().isBefore(end)&&slot.getEndTime().isAfter(start);}
}
