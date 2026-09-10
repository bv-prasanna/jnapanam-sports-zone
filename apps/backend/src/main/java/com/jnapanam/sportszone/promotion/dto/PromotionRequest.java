package com.jnapanam.sportszone.promotion.dto;

import com.jnapanam.sportszone.payment.model.PaymentPurpose;
import com.jnapanam.sportszone.promotion.model.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record PromotionRequest(
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @NotNull DiscountType discountType,
        @NotNull @DecimalMin("0.01") BigDecimal discountValue,
        @Positive Long maxDiscountPaise,
        @PositiveOrZero long minOrderPaise,
        Set<PaymentPurpose> purposes,
        boolean automatic,
        boolean onlineOnly,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @Positive Integer globalUsageLimit,
        @Min(1) int perUserLimit,
        boolean active) {}
