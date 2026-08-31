package com.jnapanam.sportszone.catalog.dto;

import com.jnapanam.sportszone.catalog.model.FacilityOperatingHour;
import java.time.*;

public record OperatingHourResponse(DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime, boolean closed) {
    public static OperatingHourResponse from(FacilityOperatingHour value) {
        return new OperatingHourResponse(value.getDayOfWeek(), value.getOpenTime(), value.getCloseTime(), value.isClosed());
    }
}

