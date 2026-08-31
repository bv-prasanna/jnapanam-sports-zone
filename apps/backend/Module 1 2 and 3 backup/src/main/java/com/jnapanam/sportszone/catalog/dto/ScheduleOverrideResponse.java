package com.jnapanam.sportszone.catalog.dto;

import com.jnapanam.sportszone.catalog.model.FacilityScheduleOverride;
import java.time.*;
import java.util.UUID;

public record ScheduleOverrideResponse(UUID id, LocalDate scheduleDate, LocalTime startTime, LocalTime endTime,
                                       String availability, Integer slotDurationMinutes, Integer capacity, String reason) {
    public static ScheduleOverrideResponse from(FacilityScheduleOverride value) {
        return new ScheduleOverrideResponse(value.getId(), value.getScheduleDate(), value.getStartTime(), value.getEndTime(),
                value.getAvailability().name(), value.getSlotDurationMinutes(), value.getCapacity(), value.getReason());
    }
}
