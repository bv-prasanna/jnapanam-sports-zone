package com.jnapanam.sportszone.catalog.dto;

import com.jnapanam.sportszone.catalog.model.Sport;
import java.time.LocalDate;
import java.util.UUID;

public record SportResponse(UUID id, String code, String name, String description, String iconKey, int displayOrder,
                            String status, LocalDate suspendedFrom, LocalDate suspendedUntil, String statusReason) {
    public static SportResponse from(Sport sport) {
        return new SportResponse(sport.getId(), sport.getCode(), sport.getName(), sport.getDescription(), sport.getIconKey(),
                sport.getDisplayOrder(), sport.getStatus().name(), sport.getSuspendedFrom(), sport.getSuspendedUntil(), sport.getStatusReason());
    }
}

