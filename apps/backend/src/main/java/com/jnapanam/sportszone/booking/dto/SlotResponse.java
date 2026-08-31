package com.jnapanam.sportszone.booking.dto;
import com.jnapanam.sportszone.booking.model.BookingSlot;
import java.time.*;
import java.util.UUID;
public record SlotResponse(UUID id,UUID facilityId,String facilityCode,LocalDate date,LocalTime startTime,LocalTime endTime,int capacity,int available){
 public static SlotResponse from(BookingSlot s){return new SlotResponse(s.getId(),s.getFacility().getId(),s.getFacility().getCode(),s.getDate(),s.getStartTime(),s.getEndTime(),s.getCapacity(),s.available());}
}
