package com.jnapanam.sportszone.catalog.dto;

import com.jnapanam.sportszone.catalog.model.FacilityClosure;
import java.time.LocalDate;
import java.util.UUID;

public record ClosureResponse(UUID id, LocalDate closureDate, String reason) {
    public static ClosureResponse from(FacilityClosure value) { return new ClosureResponse(value.getId(), value.getClosureDate(), value.getReason()); }
}

