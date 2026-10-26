package com.jnapanam.sportszone.identity.controller;

import com.jnapanam.sportszone.common.exception.NotFoundException;
import com.jnapanam.sportszone.common.exception.ConflictException;
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
        if(request.mobile()!=null&&!request.mobile().isBlank()
             &&!request.mobile().equals(user.getMobile())&&users.existsByMobile(request.mobile()))
            throw new ConflictException("Mobile already in use");
        user.profile(request.displayName(),request.profilePhotoUrl());
        user.contactDetails(request.mobile(),request.addressLine(),request.city(),request.state(),request.postalCode());
        return UserResponse.from(users.save(user));
    }
}
