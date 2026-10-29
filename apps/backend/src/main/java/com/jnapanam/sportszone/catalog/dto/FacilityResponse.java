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
                facility.getSport().getCode(), facility.getSport().getName(), effectiveStatus(facility),
                effectiveFrom(facility), effectiveUntil(facility), effectiveReason(facility));
    }
    private static boolean inherited(Facility f) {
        return f.getStatus() == com.jnapanam.sportszone.catalog.model.CatalogueStatus.ACTIVE
                && f.getSport().getStatus() != com.jnapanam.sportszone.catalog.model.CatalogueStatus.ACTIVE;
    }
    private static String effectiveStatus(Facility f) {
        return inherited(f) ? f.getSport().getStatus().name() : f.getStatus().name();
    }
    private static LocalDate effectiveFrom(Facility f) {
        return inherited(f) ? f.getSport().getSuspendedFrom() : f.getSuspendedFrom();
    }
    private static LocalDate effectiveUntil(Facility f) {
        return inherited(f) ? f.getSport().getSuspendedUntil() : f.getSuspendedUntil();
    }
    private static String effectiveReason(Facility f) {
        return inherited(f) ? "Parent sport unavailable" : f.getStatusReason();
    }
}

