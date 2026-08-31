package com.jnapanam.sportszone.identity.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 180) String email,
        @Pattern(regexp = "^[0-9+ -]{10,20}$", message = "mobile must contain 10 to 20 valid characters") String mobile,
        @NotBlank @Size(min = 8, max = 72) String password) {}
