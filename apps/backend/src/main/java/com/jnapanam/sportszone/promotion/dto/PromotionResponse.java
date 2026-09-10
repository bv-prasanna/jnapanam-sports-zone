package com.jnapanam.sportszone.promotion.dto;

import com.jnapanam.sportszone.payment.model.PaymentPurpose;
import com.jnapanam.sportszone.promotion.model.DiscountType;
import com.jnapanam.sportszone.promotion.model.Promotion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record PromotionResponse(
        UUID id, String code, String name, String description, DiscountType discountType,
        BigDecimal discountValue, Long maxDiscountPaise, long minOrderPaise,
        Set<PaymentPurpose> purposes, boolean automatic, boolean onlineOnly,
        Instant startsAt, Instant endsAt, Integer globalUsageLimit, int perUserLimit,
        int usageCount, boolean active) {
    public static PromotionResponse from(Promotion p) {
        return new PromotionResponse(p.getId(), p.getCode(), p.getName(), p.getDescription(),
                p.getDiscountType(), p.getDiscountValue(), p.getMaxDiscountPaise(),
                p.getMinOrderPaise(), p.getPurposes(), p.isAutomatic(), p.isOnlineOnly(),
                p.getStartsAt(), p.getEndsAt(), p.getGlobalUsageLimit(), p.getPerUserLimit(),
                p.getUsageCount(), p.isActive());
    }
}
