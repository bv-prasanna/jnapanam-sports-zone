package com.jnapanam.sportszone.security;

import java.util.Base64;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class ProductionJwtSecretGuardTest {
    private final SecurityConfig config = new SecurityConfig();

    @Test
    void productionRefusesKnownSourceControlledJwtSecret() {
        var env = new MockEnvironment();
        env.setActiveProfiles("prod");
        var props = new SecurityProperties(SecurityConfig.DEVELOPMENT_JWT_SECRET, 60, 30);
        var error = assertThrows(IllegalStateException.class, () -> config.jwtSecretKey(props, env));
        assertTrue(error.getMessage().contains("Production JWT_SECRET"));
    }

    @Test
    void productionAcceptsDifferentLongSigningSecret() {
        var env = new MockEnvironment();
        env.setActiveProfiles("production");
        String uniqueSecret = Base64.getEncoder().encodeToString(new byte[48]);
        SecretKey key = config.jwtSecretKey(new SecurityProperties(uniqueSecret, 60, 30), env);
        assertEquals("HmacSHA256", key.getAlgorithm());
    }

    @Test
    void localTestEnvironmentStillAcceptsDevelopmentKey() {
        var env = new MockEnvironment();
        env.setActiveProfiles("test");
        SecretKey key = config.jwtSecretKey(
                new SecurityProperties(SecurityConfig.DEVELOPMENT_JWT_SECRET, 60, 30), env);
        assertEquals("HmacSHA256", key.getAlgorithm());
    }
}
