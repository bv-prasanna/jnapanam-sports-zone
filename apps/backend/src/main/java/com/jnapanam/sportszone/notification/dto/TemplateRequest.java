package com.jnapanam.sportszone.notification.dto;

import com.jnapanam.sportszone.notification.model.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record TemplateRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{2,60}$") String code,
        @NotEmpty Set<NotificationChannel> channels,
        @Size(max = 180) String subject,
        @NotBlank String body,
        boolean active) {}
