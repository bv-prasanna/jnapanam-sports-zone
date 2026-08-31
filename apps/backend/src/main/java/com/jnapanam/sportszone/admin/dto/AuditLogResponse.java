package com.jnapanam.sportszone.admin.dto;

import com.jnapanam.sportszone.admin.model.AuditLog;
import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id, UUID actorUserId, String method, String path, int responseStatus,
        String remoteAddress, Instant occurredAt, String action, String resourceType,
        String resourceId, String queryString, String correlationId, String userAgent,
        Long durationMs, boolean successful) {
    public static AuditLogResponse from(AuditLog a) {
        return new AuditLogResponse(a.getId(), a.getActorUserId(), a.getHttpMethod(),
                a.getRequestPath(), a.getResponseStatus(), a.getRemoteAddress(),
                a.getOccurredAt(), a.getAction(), a.getResourceType(), a.getResourceId(),
                a.getQueryString(), a.getCorrelationId(), a.getUserAgent(),
                a.getDurationMs(), a.isSuccessful());
    }
}
