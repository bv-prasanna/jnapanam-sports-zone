package com.jnapanam.sportszone.security;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

class AuthRateLimitFilterTest {
    @Test
    void forgedForwardedForCannotBypassLoginRateLimit() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(5);
        AtomicInteger passed = new AtomicInteger();
        for (int i = 0; i < 6; i++) {
            var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            request.setRemoteAddr("192.0.2.15");
            request.addHeader("X-Forwarded-For", "198.51.100." + (10 + i));
            request.setAttribute("jnapanam.correlationId", "support-ref-123456");
            var response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, (req, res) -> passed.incrementAndGet());
            if (i < 5) {
                assertEquals(200, response.getStatus());
            } else {
                assertEquals(429, response.getStatus());
                assertEquals("AUTH_RATE_LIMITED", request.getAttribute("jnapanam.errorCode"));
                assertEquals("application/problem+json", response.getContentType());
                var body = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(response.getContentAsString());
                assertEquals("AUTH_RATE_LIMITED", body.path("code").asText());
                assertEquals("support-ref-123456", body.path("requestId").asText());
                assertNotNull(response.getHeader("Retry-After"));
            }
        }
        assertEquals(5, passed.get());
    }

    @Test
    void rateLimiterOnlyCoversSensitivePostRoutes() {
        var filter = new AuthRateLimitFilter(5);
        assertFalse(filter.shouldNotFilter(new MockHttpServletRequest("POST", "/api/v1/auth/login")));
        assertFalse(filter.shouldNotFilter(new MockHttpServletRequest("POST", "/api/v1/auth/password/reset")));
        assertTrue(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/api/v1/auth/login")));
        assertTrue(filter.shouldNotFilter(new MockHttpServletRequest("POST", "/api/v1/bookings")));
    }

    @Test
    void limiterIsInsideCorrelationTelemetry() {
        assertEquals(-101, AuthRateLimitFilter.class.getAnnotation(Order.class).value());
    }
}
