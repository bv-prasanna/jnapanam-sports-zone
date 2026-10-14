package com.jnapanam.sportszone.identity.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.jnapanam.sportszone.identity.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.server.ResponseStatusException;

class AuthControllerProfileTest {
    @Test
    void legacyRegistrationIsDisabledWhenNoDevelopmentProfileIsActive() {
        var controller = new AuthController(mock(AuthService.class), new MockEnvironment());
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> controller.register(null));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void productionProfileOverridesAccidentallyEnabledDevelopmentProfile() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("uat", "production");
        var controller = new AuthController(mock(AuthService.class), environment);
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> controller.register(null));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void testProfileRetainsLegacyRegistrationForSeedData() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        var controller = new AuthController(mock(AuthService.class), environment);
        assertEquals(HttpStatus.CREATED, controller.register(null).getStatusCode());
    }
}
