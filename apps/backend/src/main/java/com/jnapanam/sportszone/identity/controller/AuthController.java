package com.jnapanam.sportszone.identity.controller;

import com.jnapanam.sportszone.identity.dto.*;
import com.jnapanam.sportszone.identity.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/auth") @SecurityRequirements
public class AuthController {
    private final AuthService auth;
    private final boolean legacyRegistrationAllowed;
    public AuthController(AuthService auth, Environment environment) {
        this.auth = auth;
        // Existing seed/test registration remains available to local and integration tests.
        // Production must never mint an active account without a consumed SMS OTP challenge.
        this.legacyRegistrationAllowed =
                environment.acceptsProfiles(Profiles.of("test", "dev", "local", "uat"))
                && !environment.acceptsProfiles(Profiles.of("prod", "production"));
    }
    @PostMapping("/register")
    ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (!legacyRegistrationAllowed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Use verified mobile registration");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(request));
    }
    @PostMapping("/login") TokenResponse login(@Valid @RequestBody LoginRequest request) { return auth.login(request); }
    @PostMapping("/refresh") TokenResponse refresh(@Valid @RequestBody RefreshRequest request) { return auth.refresh(request); }
    @PostMapping("/logout") ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) { auth.logout(request); return ResponseEntity.noContent().build(); }
}
