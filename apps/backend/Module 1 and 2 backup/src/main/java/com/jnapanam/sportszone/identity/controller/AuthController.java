package com.jnapanam.sportszone.identity.controller;

import com.jnapanam.sportszone.identity.dto.*;
import com.jnapanam.sportszone.identity.service.AuthService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/auth") @SecurityRequirements
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }
    @PostMapping("/register") ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(request)); }
    @PostMapping("/login") TokenResponse login(@Valid @RequestBody LoginRequest request) { return auth.login(request); }
    @PostMapping("/refresh") TokenResponse refresh(@Valid @RequestBody RefreshRequest request) { return auth.refresh(request); }
    @PostMapping("/logout") ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) { auth.logout(request); return ResponseEntity.noContent().build(); }
}
