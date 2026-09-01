package com.jnapanam.sportszone.loyalty.dto;import jakarta.validation.constraints.*;public record LoyaltyAdjustmentRequest(@NotNull Long points,@NotBlank@Size(max=250)String reason){}
