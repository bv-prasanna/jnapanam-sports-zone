package com.jnapanam.sportszone.identity.controller;

import com.jnapanam.sportszone.identity.dto.ChangePasswordRequest;
import com.jnapanam.sportszone.identity.service.ChangePasswordService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile/password")
public class ChangePasswordController {
    private final ChangePasswordService service;

    public ChangePasswordController(ChangePasswordService service) {
        this.service = service;
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void change(@AuthenticationPrincipal Jwt jwt,
                       @Valid @RequestBody ChangePasswordRequest request) {
        service.change(UUID.fromString(jwt.getSubject()), request);
    }
}
