package com.jnapanam.sportszone.identity.dto;import jakarta.validation.constraints.*;public record ForgotPasswordRequest(@NotBlank@Email String email){}
