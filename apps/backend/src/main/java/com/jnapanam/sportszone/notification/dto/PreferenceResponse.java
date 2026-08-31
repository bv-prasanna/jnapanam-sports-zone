package com.jnapanam.sportszone.notification.dto;import java.util.UUID;public record PreferenceResponse(UUID userId,boolean emailEnabled,boolean smsEnabled,boolean pushEnabled){}
