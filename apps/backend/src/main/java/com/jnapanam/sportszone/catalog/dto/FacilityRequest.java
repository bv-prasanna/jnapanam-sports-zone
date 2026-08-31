package com.jnapanam.sportszone.catalog.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record FacilityRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{2,50}$") String code,
        @NotBlank @Size(max = 40) String sportCode,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @Size(max = 160) String locationLabel,
        @Min(15) @Max(240) int slotDurationMinutes,
        @Min(1) int capacity,
        @DecimalMin("0.00") BigDecimal bookingPrice) {}
