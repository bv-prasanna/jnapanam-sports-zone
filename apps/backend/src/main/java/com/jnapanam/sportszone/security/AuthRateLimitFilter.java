package com.jnapanam.sportszone.security;

import com.jnapanam.sportszone.admin.web.ApiRequestTelemetryFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * In-process login/reset throttling. Caller-supplied X-Forwarded-For is never
 * trusted for a rate-limit key. A trusted edge/distributed limiter is additionally
 * required when behind a reverse proxy or running multiple application instances.
 *
 * Runs inside -102 telemetry and ahead of -99 admin audit so early 429s carry
 * correlation references and can be recorded.
 */
@Component
@Order(-101)
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private final ConcurrentHashMap<String, Window> requests = new ConcurrentHashMap<>();
    private final int limit;

    public AuthRateLimitFilter(@Value("${app.security.auth-rate-limit-per-minute:60}") int limit) {
        this.limit = Math.max(5, limit);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) return true;
        String path = request.getRequestURI();
        return !(path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/password/forgot")
                || path.equals("/api/v1/auth/password/reset"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long now = Instant.now().getEpochSecond();
        long minute = now / 60;
        String remoteAddress = request.getRemoteAddr();
        String key = (remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress)
                + ":" + request.getRequestURI();
        Window window = requests.compute(key, (ignored, previous) ->
                previous == null || previous.minute() != minute
                        ? new Window(minute, 1) : new Window(minute, previous.count() + 1));

        if (window.count() > limit) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(Math.max(1, 60 - now % 60)));
            request.setAttribute("jnapanam.errorCode", "AUTH_RATE_LIMITED");
            Object candidate = request.getAttribute(ApiRequestTelemetryFilter.CORRELATION_ATTRIBUTE);
            String reference = candidate instanceof String id && id.matches("[A-Za-z0-9_-]{8,80}")
                    ? ",\"requestId\":\"" + id + "\"" : "";
            response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Too Many Requests\","
                    + "\"status\":429,\"code\":\"AUTH_RATE_LIMITED\","
                    + "\"detail\":\"Too many sign-in attempts. Please try again shortly.\"" + reference + "}");
            return;
        }

        chain.doFilter(request, response);
        if (requests.size() > 10_000)
            requests.entrySet().removeIf(entry -> entry.getValue().minute() < minute - 2);
    }

    private record Window(long minute, int count) {}
}
