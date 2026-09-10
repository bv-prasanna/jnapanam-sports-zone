package com.jnapanam.sportszone.notification.dto;

import com.jnapanam.sportszone.notification.model.NotificationChannel;
import com.jnapanam.sportszone.notification.model.NotificationTemplate;
import java.util.Set;
import java.util.UUID;

public record TemplateResponse(
        UUID id, String code, Set<NotificationChannel> channels, String subject, String body, boolean active) {
    public static TemplateResponse from(NotificationTemplate template) {
        return new TemplateResponse(template.getId(), template.getCode(), template.getChannels(),
                template.getSubject(), template.getBody(), template.isActive());
    }
}
