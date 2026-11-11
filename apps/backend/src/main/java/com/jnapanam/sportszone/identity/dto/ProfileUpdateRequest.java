package com.jnapanam.sportszone.identity.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
    @Size(max = 60, message = "Display name must be 60 characters or fewer") String displayName,
    @Size(max = 500, message = "Photo URL must be 500 characters or fewer")
    @Pattern(regexp = "^$|https?://.+", message = "Photo URL must begin with http:// or https://") String profilePhotoUrl,
    @Pattern(regexp="^$|[0-9]{10}") String mobile,
    @Size(max=250) String addressLine,
    @Size(max=100) String city,
    @Size(max=100) String state,
    @Size(max=12) String postalCode
) {}
