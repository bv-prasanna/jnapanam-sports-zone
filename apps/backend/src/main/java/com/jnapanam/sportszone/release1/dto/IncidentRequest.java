package com.jnapanam.sportszone.release1.dto;
import jakarta.validation.constraints.*;import java.util.UUID;
public record IncidentRequest(UUID facilityId,UUID bookingId,@NotBlank @Size(max=40)String incidentType,@NotBlank @Size(max=20)String severity,@NotBlank @Size(max=3000)String description){}
