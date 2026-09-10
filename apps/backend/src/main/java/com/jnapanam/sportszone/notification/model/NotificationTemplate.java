package com.jnapanam.sportszone.notification.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "notification_templates")
public class NotificationTemplate {
    @Id private UUID id;
    @Column(nullable = false, unique = true, length = 60) private String code;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private NotificationChannel channel;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "notification_template_channels", joinColumns = @JoinColumn(name = "template_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20)
    private Set<NotificationChannel> channels = new LinkedHashSet<>();
    @Column(length = 180) private String subject;
    @Column(nullable = false, columnDefinition = "TEXT") private String body;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected NotificationTemplate() {}

    public NotificationTemplate(String code, Set<NotificationChannel> channels, String subject, String body) {
        id = UUID.randomUUID();
        this.code = code.trim().toUpperCase(Locale.ROOT);
        update(channels, subject, body, true);
        createdAt = Instant.now();
    }

    public void update(Set<NotificationChannel> channels, String subject, String body, boolean active) {
        this.channels.clear();
        if (channels != null) this.channels.addAll(channels);
        if (this.channels.isEmpty()) throw new IllegalArgumentException("Select at least one notification channel");
        this.channel = this.channels.iterator().next();
        this.subject = clean(subject);
        this.body = body.trim();
        this.active = active;
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public NotificationChannel getChannel() { return channel; }
    public Set<NotificationChannel> getChannels() { return Set.copyOf(channels); }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public boolean isActive() { return active; }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
