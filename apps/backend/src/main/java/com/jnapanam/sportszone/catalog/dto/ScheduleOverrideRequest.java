package com.jnapanam.sportszone.catalog.dto;

import com.jnapanam.sportszone.catalog.model.Availability;
import jakarta.validation.constraints.*;
import java.time.*;

public record ScheduleOverrideRequest(
        @NotNull LocalDate scheduleDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull Availability availability,
        @Min(15) @Max(240) Integer slotDurationMinutes,
        @Min(1) Integer capacity,
        @Size(max = 250) String reason) {}

