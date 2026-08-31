package com.jnapanam.sportszone.payment.dto;import jakarta.validation.constraints.*;public record RefundRequest(@Positive Long amountPaise,@NotBlank @Size(max=300) String reason){}
