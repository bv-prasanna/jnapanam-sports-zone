package com.jnapanam.sportszone.booking.dto;
import com.jnapanam.sportszone.booking.model.BookingHold;
import java.time.Instant;
import java.util.UUID;
public record HoldResponse(UUID id,UUID slotId,int quantity,String status,Instant expiresAt){public static HoldResponse from(BookingHold h){return new HoldResponse(h.getId(),h.getSlot().getId(),h.getQuantity(),h.getStatus().name(),h.getExpiresAt());}}
