package com.jnapanam.sportszone.identity.dto;

import com.jnapanam.sportszone.identity.model.User;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record UserResponse(UUID id, String fullName, String displayName, String profilePhotoUrl, String email, String mobile, String status, Set<String> roles, boolean requiresUpfrontPayment) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getDisplayName(), user.getProfilePhotoUrl(), user.getEmail(), user.getMobile(), user.getStatus().name(),
                user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toUnmodifiableSet()), user.isRequiresUpfrontPayment());
    }
}
