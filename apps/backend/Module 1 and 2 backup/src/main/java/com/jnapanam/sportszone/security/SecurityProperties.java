package com.jnapanam.sportszone.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(String jwtSecret, long accessTokenMinutes, long refreshTokenDays) {}
