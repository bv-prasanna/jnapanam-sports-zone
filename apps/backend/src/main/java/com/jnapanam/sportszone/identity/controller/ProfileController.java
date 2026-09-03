package com.jnapanam.sportszone.identity.controller;

import com.jnapanam.sportszone.common.exception.NotFoundException;
import com.jnapanam.sportszone.identity.dto.UserResponse;
import com.jnapanam.sportszone.identity.dto.ProfileUpdateRequest;
import jakarta.validation.Valid;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/profile")
public class ProfileController {
    private final UserRepository users;
    public ProfileController(UserRepository users) { this.users = users; }
    @GetMapping("/me") UserResponse me(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        return users.findById(UUID.fromString(jwt.getSubject())).map(UserResponse::from).orElseThrow(() -> new NotFoundException("User not found"));
    }
    @PutMapping("/me") UserResponse update(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt,@Valid @RequestBody ProfileUpdateRequest request) {
        var user=users.findById(UUID.fromString(jwt.getSubject())).orElseThrow(() -> new NotFoundException("User not found"));
        user.profile(request.displayName(),request.profilePhotoUrl());
        return UserResponse.from(users.save(user));
    }
}
