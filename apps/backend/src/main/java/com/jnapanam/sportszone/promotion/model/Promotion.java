package com.jnapanam.sportszone.promotion.model;

import com.jnapanam.sportszone.payment.model.PaymentPurpose;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "promotions")
public class Promotion {
    @Id private UUID id;
    @Column(nullable = false, unique = true, length = 40) private String code;
    @Column(nullable = false, length = 120) private String name;
    @Column(length = 500) private String description;
    @Enumerated(EnumType.STRING) @Column(name = "discount_type", nullable = false, length = 20) private DiscountType discountType;
    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2) private BigDecimal discountValue;
    @Column(name = "max_discount_paise") private Long maxDiscountPaise;
    @Column(name = "min_order_paise", nullable = false) private long minOrderPaise;
    @Enumerated(EnumType.STRING) @Column(length = 20) private PaymentPurpose purpose;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "promotion_purposes", joinColumns = @JoinColumn(name = "promotion_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", length = 20)
    private Set<PaymentPurpose> purposes = new LinkedHashSet<>();
    @Column(nullable = false) private boolean automatic;
    @Column(name = "online_only", nullable = false) private boolean onlineOnly;
    @Column(name = "starts_at", nullable = false) private Instant startsAt;
    @Column(name = "ends_at", nullable = false) private Instant endsAt;
    @Column(name = "global_usage_limit") private Integer globalUsageLimit;
    @Column(name = "per_user_limit", nullable = false) private int perUserLimit;
    @Column(name = "usage_count", nullable = false) private int usageCount;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private long version;

    protected Promotion() {}

    public Promotion(String code, String name, String description, DiscountType type, BigDecimal value,
            Long max, long min, Set<PaymentPurpose> purposes, boolean automatic, boolean onlineOnly,
            Instant starts, Instant ends, Integer global, int perUser) {
        id = UUID.randomUUID();
        update(code, name, description, type, value, max, min, purposes, automatic, onlineOnly,
                starts, ends, global, perUser, true);
        createdAt = Instant.now();
    }

    public void update(String code, String name, String description, DiscountType type, BigDecimal value,
            Long max, long min, Set<PaymentPurpose> purposes, boolean automatic, boolean onlineOnly,
            Instant starts, Instant ends, Integer global, int perUser, boolean active) {
        this.code = code.trim().toUpperCase();
        this.name = name.trim();
        this.description = description;
        discountType = type;
        discountValue = value;
        maxDiscountPaise = max;
        minOrderPaise = min;
        this.purposes.clear();
        if (purposes != null) this.purposes.addAll(purposes);
        purpose = this.purposes.size() == 1 ? this.purposes.iterator().next() : null;
        this.automatic = automatic;
        this.onlineOnly = onlineOnly;
        startsAt = starts;
        endsAt = ends;
        globalUsageLimit = global;
        perUserLimit = perUser;
        this.active = active;
        updatedAt = Instant.now();
    }

    public boolean eligible(long amount, PaymentPurpose requestedPurpose, Instant now) {
        boolean purposeAllowed = purposes.isEmpty() || purposes.contains(requestedPurpose);
        return active && !now.isBefore(startsAt) && now.isBefore(endsAt) && amount >= minOrderPaise
                && purposeAllowed && (globalUsageLimit == null || usageCount < globalUsageLimit);
    }

    public long discount(long amount) {
        BigDecimal result = discountType == DiscountType.PERCENTAGE
                ? BigDecimal.valueOf(amount).multiply(discountValue).divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
                : discountValue.movePointRight(2);
        long value = result.longValue();
        if (maxDiscountPaise != null) value = Math.min(value, maxDiscountPaise);
        return Math.min(value, Math.max(0, amount - 1));
    }

    public void redeemed() { usageCount++; updatedAt = Instant.now(); }
    public void reverseRedemption() { if (usageCount > 0) usageCount--; updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public DiscountType getDiscountType() { return discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public Long getMaxDiscountPaise() { return maxDiscountPaise; }
    public long getMinOrderPaise() { return minOrderPaise; }
    public PaymentPurpose getPurpose() { return purpose; }
    public Set<PaymentPurpose> getPurposes() { return Set.copyOf(purposes); }
    public boolean isAutomatic() { return automatic; }
    public boolean isOnlineOnly() { return onlineOnly; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public Integer getGlobalUsageLimit() { return globalUsageLimit; }
    public int getPerUserLimit() { return perUserLimit; }
    public int getUsageCount() { return usageCount; }
    public boolean isActive() { return active; }
}
