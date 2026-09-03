package com.jnapanam.sportszone.booking.dto;

import jakarta.validation.constraints.Size;

public record BookingCheckInRequest(
    boolean identityVerified,
    @Size(max = 120, message = "Verification note must be 120 characters or fewer") String verificationNote
) {}
