package com.jnapanam.sportszone.catalog.dto;

import jakarta.validation.constraints.NotNull;
import java.time.*;

public record OperatingHourRequest(@NotNull DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime, boolean closed) {}

