package com.jnapanam.sportszone.usermanagement.dto;import jakarta.validation.constraints.*;public record AdminPasswordResetRequest(@NotBlank@Size(min=10,max=100)String temporaryPassword){}
