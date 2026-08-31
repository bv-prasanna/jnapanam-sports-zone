package com.jnapanam.sportszone.catalog.dto;

import jakarta.validation.constraints.*;

public record SportRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{2,40}$") String code,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @Size(max = 60) String iconKey,
        @Min(0) int displayOrder) {}

