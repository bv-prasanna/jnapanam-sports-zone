package com.jnapanam.sportszone.release1.dto;
import jakarta.validation.constraints.*;
public record SupportTicketRequest(@NotBlank @Size(max=40)String category,@NotBlank @Size(max=180)String subject,@NotBlank @Size(max=3000)String description,@Size(max=20)String priority){}
