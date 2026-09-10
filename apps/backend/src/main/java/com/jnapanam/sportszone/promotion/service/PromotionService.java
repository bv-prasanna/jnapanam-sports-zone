package com.jnapanam.sportszone.promotion.service;

import com.jnapanam.sportszone.common.exception.BadRequestException;
import com.jnapanam.sportszone.common.exception.ConflictException;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import com.jnapanam.sportszone.identity.model.User;
import com.jnapanam.sportszone.payment.model.PaymentOrder;
import com.jnapanam.sportszone.payment.model.PaymentPurpose;
import com.jnapanam.sportszone.promotion.dto.PromotionRequest;
import com.jnapanam.sportszone.promotion.dto.PromotionResponse;
import com.jnapanam.sportszone.promotion.model.DiscountType;
import com.jnapanam.sportszone.promotion.model.Promotion;
import com.jnapanam.sportszone.promotion.model.PromotionRedemption;
import com.jnapanam.sportszone.promotion.repository.PromotionRedemptionRepository;
import com.jnapanam.sportszone.promotion.repository.PromotionRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromotionService {
    private final PromotionRepository promotions;
    private final PromotionRedemptionRepository redemptions;

    public PromotionService(PromotionRepository promotions, PromotionRedemptionRepository redemptions) {
        this.promotions = promotions;
        this.redemptions = redemptions;
    }

    @Transactional
    public PromotionResponse create(PromotionRequest request) {
        if (promotions.findByCodeIgnoreCase(request.code()).isPresent()) {
            throw new ConflictException("Promotion code already exists");
        }
        validate(request);
        Promotion promotion = new Promotion(request.code(), request.name(), request.description(),
                request.discountType(), request.discountValue(), request.maxDiscountPaise(),
                request.minOrderPaise(), request.purposes(), request.automatic(), request.onlineOnly(),
                request.startsAt(), request.endsAt(), request.globalUsageLimit(), request.perUserLimit());
        if (!request.active()) apply(promotion, request, false);
        return PromotionResponse.from(promotions.save(promotion));
    }

    @Transactional
    public PromotionResponse update(UUID id, PromotionRequest request) {
        validate(request);
        Promotion promotion = get(id);
        promotions.findByCodeIgnoreCase(request.code()).filter(x -> !x.getId().equals(id)).ifPresent(x -> {
            throw new ConflictException("Promotion code already exists");
        });
        apply(promotion, request, request.active());
        return PromotionResponse.from(promotion);
    }

    @Transactional(readOnly = true)
    public List<PromotionResponse> all() {
        return promotions.findAllByOrderByCreatedAtDesc().stream().map(PromotionResponse::from).toList();
    }

    public Applied choose(String code, long amount, PaymentPurpose purpose, User user) {
        Instant now = Instant.now();
        Promotion promotion;
        if (code != null && !code.isBlank()) {
            promotion = promotions.findByCodeIgnoreCase(code.trim())
                    .orElseThrow(() -> new BadRequestException("Invalid coupon code"));
        } else {
            promotion = promotions.findByActiveTrueAndAutomaticTrueAndStartsAtLessThanEqualAndEndsAtAfter(now, now)
                    .stream().filter(x -> x.eligible(amount, purpose, now))
                    .max(Comparator.comparingLong(x -> x.discount(amount))).orElse(null);
        }
        if (promotion == null) return new Applied(null, 0);
        if (!promotion.eligible(amount, purpose, now)) {
            throw new ConflictException("Promotion is not currently applicable");
        }
        if (redemptions.countByPromotionIdAndUserId(promotion.getId(), user.getId()) >= promotion.getPerUserLimit()) {
            throw new ConflictException("Promotion usage limit reached");
        }
        return new Applied(promotion, promotion.discount(amount));
    }

    public void redeem(Applied applied, User user, PaymentOrder order) {
        if (applied.promotion() != null && applied.discountPaise() > 0) {
            applied.promotion().redeemed();
            redemptions.save(new PromotionRedemption(applied.promotion(), user, order, applied.discountPaise()));
        }
    }

    @Transactional
    public void reverseRedemption(PaymentOrder order) {
        redemptions.findByPaymentOrderId(order.getId()).ifPresent(redemption -> {
            redemption.getPromotion().reverseRedemption();
            redemptions.delete(redemption);
        });
    }

    private Promotion get(UUID id) {
        return promotions.findById(id).orElseThrow(() -> new NotFoundException("Promotion not found"));
    }

    private static void apply(Promotion promotion, PromotionRequest request, boolean active) {
        promotion.update(request.code(), request.name(), request.description(), request.discountType(),
                request.discountValue(), request.maxDiscountPaise(), request.minOrderPaise(), request.purposes(),
                request.automatic(), request.onlineOnly(), request.startsAt(), request.endsAt(),
                request.globalUsageLimit(), request.perUserLimit(), active);
    }

    private static void validate(PromotionRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new BadRequestException("Promotion end must be after start");
        }
        if (request.discountType() == DiscountType.PERCENTAGE
                && request.discountValue().compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("Percentage cannot exceed 100");
        }
    }

    public record Applied(Promotion promotion, long discountPaise) {
        public String code() { return promotion == null ? null : promotion.getCode(); }
    }
}
