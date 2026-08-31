package com.jnapanam.sportszone.identity.dto;

import java.time.Instant;

public record TokenResponse(String tokenType, String accessToken, Instant accessTokenExpiresAt, String refreshToken) {}
