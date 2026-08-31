package com.jnapanam.sportszone.catalog.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record ClosureRequest(@NotNull LocalDate closureDate, @NotBlank @Size(max = 250) String reason) {}

