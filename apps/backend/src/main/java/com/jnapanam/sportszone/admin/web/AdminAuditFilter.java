package com.jnapanam.sportszone.admin.web;

import com.jnapanam.sportszone.admin.model.AuditLog;
import com.jnapanam.sportszone.admin.repository.AuditLogRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(-99)
public class AdminAuditFilter extends OncePerRequestFilter {
    private static final Set<String> MUTATING = Set.of("POST", "PUT", "PATCH", "DELETE");
    private final AuditLogRepository logs;
    public AdminAuditFilter(AuditLogRepository logs) { this.logs = logs; }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean auditedPath = path.startsWith("/api/v1/");
        return !MUTATING.contains(request.getMethod()) || !auditedPath;
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                               FilterChain chain) throws ServletException, IOException {
        long started = System.nanoTime();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID actor = actor(authentication);
        String correlationId = headerOrNew(request, "X-Correlation-ID");
        response.setHeader("X-Correlation-ID", correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            try {
                Resource resource = resource(request.getRequestURI());
                AuditLog event = new AuditLog(actor, request.getMethod(), request.getRequestURI(), response.getStatus(),
                        clientAddress(request), request.getMethod() + "_" + resource.type(), resource.type(),
                        resource.id(), limited(request.getQueryString(), 1000), correlationId,
                        limited(request.getHeader("User-Agent"), 300),
                        (System.nanoTime() - started) / 1_000_000);
                event.recordLocation(request.getHeader("X-Client-Latitude"), request.getHeader("X-Client-Longitude"),request.getHeader("X-Location-Status"));
                logs.save(event);
            } catch (Exception ignored) {
                // An audit persistence problem must not change the business response.
            }
        }
    }

    private static UUID actor(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        try { return UUID.fromString(authentication.getName()); } catch (Exception ignored) { return null; }
    }

    private static String headerOrNew(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : limited(value, 80);
    }

    private static String clientAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : limited(forwarded.split(",")[0].trim(), 80);
    }

    private static Resource resource(String path) {
        String[] parts = path.split("/");
        String type = parts.length > 4 ? parts[4].replace('-', '_').toUpperCase(Locale.ROOT) : "ADMIN_API";
        String id = null;
        for (int i = 5; i < parts.length; i++) {
            if (parts[i].matches("[0-9a-fA-F-]{36}")) { id = parts[i]; break; }
        }
        return new Resource(type, id);
    }

    private static String limited(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private record Resource(String type, String id) {}
}
