package com.jnapanam.sportszone.catalog.dto;

import com.jnapanam.sportszone.catalog.model.CatalogueStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record StatusRequest(
        @NotNull CatalogueStatus status,
        LocalDate suspendedFrom,
        LocalDate suspendedUntil,
        @Size(max = 250) String reason) {}

