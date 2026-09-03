package com.jnapanam.sportszone.identity.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
    @Size(max = 60, message = "Display name must be 60 characters or fewer") String displayName,
    @Size(max = 500, message = "Photo URL must be 500 characters or fewer")
    @Pattern(regexp = "^$|https?://.+", message = "Photo URL must begin with http:// or https://") String profilePhotoUrl
) {}
