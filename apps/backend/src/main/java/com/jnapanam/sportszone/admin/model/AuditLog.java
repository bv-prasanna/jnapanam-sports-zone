package com.jnapanam.sportszone.admin.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id private UUID id;
    @Column(name = "actor_user_id") private UUID actorUserId;
    @Column(name = "http_method", nullable = false, length = 10) private String httpMethod;
    @Column(name = "request_path", nullable = false, length = 300) private String requestPath;
    @Column(name = "response_status", nullable = false) private int responseStatus;
    @Column(name = "remote_address", length = 80) private String remoteAddress;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(nullable = false, length = 80) private String action;
    @Column(name = "resource_type", nullable = false, length = 80) private String resourceType;
    @Column(name = "resource_id", length = 100) private String resourceId;
    @Column(name = "query_string", length = 1000) private String queryString;
    @Column(name = "correlation_id", length = 80) private String correlationId;
    @Column(name = "user_agent", length = 300) private String userAgent;
    @Column(name = "duration_ms") private Long durationMs;
    @Column(nullable = false) private boolean successful;

    protected AuditLog() {}

    public AuditLog(UUID actor, String method, String path, int status, String remote,
                    String action, String resourceType, String resourceId, String query,
                    String correlationId, String userAgent, long durationMs) {
        this.id = UUID.randomUUID();
        this.actorUserId = actor;
        this.httpMethod = method;
        this.requestPath = path;
        this.responseStatus = status;
        this.remoteAddress = remote;
        this.occurredAt = Instant.now();
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.queryString = query;
        this.correlationId = correlationId;
        this.userAgent = userAgent;
        this.durationMs = durationMs;
        this.successful = status >= 200 && status < 400;
    }

    public UUID getId(){return id;}
    public UUID getActorUserId(){return actorUserId;}
    public String getHttpMethod(){return httpMethod;}
    public String getRequestPath(){return requestPath;}
    public int getResponseStatus(){return responseStatus;}
    public String getRemoteAddress(){return remoteAddress;}
    public Instant getOccurredAt(){return occurredAt;}
    public String getAction(){return action;}
    public String getResourceType(){return resourceType;}
    public String getResourceId(){return resourceId;}
    public String getQueryString(){return queryString;}
    public String getCorrelationId(){return correlationId;}
    public String getUserAgent(){return userAgent;}
    public Long getDurationMs(){return durationMs;}
    public boolean isSuccessful(){return successful;}
}
