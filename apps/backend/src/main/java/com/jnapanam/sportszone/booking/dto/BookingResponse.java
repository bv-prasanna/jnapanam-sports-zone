package com.jnapanam.sportszone.booking.dto;
import com.jnapanam.sportszone.booking.model.Booking;
import java.time.*;
import java.util.UUID;
public record BookingResponse(UUID id,String bookingNumber,UUID facilityId,String facilityName,UUID slotId,LocalDate date,LocalTime startTime,LocalTime endTime,int quantity,String status,Instant confirmedAt,Instant cancelledAt){
 public static BookingResponse from(Booking b){var s=b.getSlot();return new BookingResponse(b.getId(),b.getBookingNumber(),s.getFacility().getId(),s.getFacility().getName(),s.getId(),s.getDate(),s.getStartTime(),s.getEndTime(),b.getQuantity(),b.getStatus().name(),b.getConfirmedAt(),b.getCancelledAt());}
}
