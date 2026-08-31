package com.jnapanam.sportszone.catalog.dto;

import com.jnapanam.sportszone.catalog.model.Facility;
import java.time.LocalDate;
import java.util.UUID;
import java.math.BigDecimal;

public record FacilityResponse(UUID id, String code, String name, String description, String locationLabel,
                               int slotDurationMinutes, int capacity, BigDecimal bookingPrice, String sportCode, String sportName,
                               String status, LocalDate suspendedFrom, LocalDate suspendedUntil, String statusReason) {
    public static FacilityResponse from(Facility facility) {
        return new FacilityResponse(facility.getId(), facility.getCode(), facility.getName(), facility.getDescription(),
                facility.getLocationLabel(), facility.getSlotDurationMinutes(), facility.getCapacity(), facility.getBookingPrice(),
                facility.getSport().getCode(), facility.getSport().getName(), facility.getStatus().name(),
                facility.getSuspendedFrom(), facility.getSuspendedUntil(), facility.getStatusReason());
    }
}
