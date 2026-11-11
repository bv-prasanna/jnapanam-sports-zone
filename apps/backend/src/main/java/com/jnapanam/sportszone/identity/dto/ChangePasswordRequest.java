package com.jnapanam.sportszone.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Password change for an authenticated account; never accept a user id from the client. */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 12, max = 72) String newPassword) {}
