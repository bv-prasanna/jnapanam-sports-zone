package com.jnapanam.sportszone.catalog.dto;

import java.time.LocalDate;
import java.util.List;

public record FacilityScheduleResponse(
        LocalDate from,
        LocalDate to,
        List<OperatingHourResponse> weeklyOperatingHours,
        List<ClosureResponse> closures,
        List<ScheduleOverrideResponse> overrides) {}

