package com.jnapanam.sportszone.payment.dto;import jakarta.validation.constraints.*;public record VoidPaymentRequest(@NotBlank @Size(max=300) String reason){}
