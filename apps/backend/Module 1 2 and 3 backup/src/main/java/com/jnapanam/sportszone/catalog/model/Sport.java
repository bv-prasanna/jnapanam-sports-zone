package com.jnapanam.sportszone.catalog.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "sports")
public class Sport {
    @Id private UUID id;
    @Column(nullable = false, unique = true, length = 40) private String code;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 500) private String description;
    @Column(name = "icon_key", length = 60) private String iconKey;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private CatalogueStatus status;
    @Column(name = "suspended_from") private LocalDate suspendedFrom;
    @Column(name = "suspended_until") private LocalDate suspendedUntil;
    @Column(name = "status_reason", length = 250) private String statusReason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Sport() {}

    public Sport(String code, String name, String description, String iconKey, int displayOrder) {
        this.id = UUID.randomUUID();
        this.code = normalizeCode(code);
        this.name = name.trim();
        this.description = trimToNull(description);
        this.iconKey = trimToNull(iconKey);
        this.displayOrder = displayOrder;
        this.status = CatalogueStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(String name, String description, String iconKey, int displayOrder) {
        this.name = name.trim();
        this.description = trimToNull(description);
        this.iconKey = trimToNull(iconKey);
        this.displayOrder = displayOrder;
        this.updatedAt = Instant.now();
    }

    public void changeStatus(CatalogueStatus status, LocalDate from, LocalDate until, String reason) {
        this.status = status;
        this.suspendedFrom = status == CatalogueStatus.TEMPORARILY_SUSPENDED ? from : null;
        this.suspendedUntil = status == CatalogueStatus.TEMPORARILY_SUSPENDED ? until : null;
        this.statusReason = status == CatalogueStatus.ACTIVE ? null : trimToNull(reason);
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIconKey() { return iconKey; }
    public int getDisplayOrder() { return displayOrder; }
    public CatalogueStatus getStatus() { return status; }
    public LocalDate getSuspendedFrom() { return suspendedFrom; }
    public LocalDate getSuspendedUntil() { return suspendedUntil; }
    public String getStatusReason() { return statusReason; }
    public boolean isAvailableOn(LocalDate date) {
        if (status == CatalogueStatus.ACTIVE) return true;
        if (status == CatalogueStatus.INACTIVE) return false;
        return date.isBefore(suspendedFrom) || date.isAfter(suspendedUntil);
    }

    private static String normalizeCode(String value) { return value.trim().toUpperCase(java.util.Locale.ROOT); }
    private static String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
