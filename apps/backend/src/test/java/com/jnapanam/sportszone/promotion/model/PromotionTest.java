package com.jnapanam.sportszone.promotion.model;

import com.jnapanam.sportszone.payment.model.PaymentPurpose;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PromotionTest {
    private static final Instant START = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-12-01T00:00:00Z");
    private Promotion promotion(DiscountType type, String value, Long cap, long minimum,
            Integer globalLimit, boolean active, Set<PaymentPurpose> purposes) {
        return new Promotion("SAVE", "Save offer", null, type, new BigDecimal(value), cap,
                minimum, purposes, false, true, START, END, globalLimit, 2);
    }

    @Test void percentageDiscountRoundsDownAndHonoursMaximumCap() {
        var offer = promotion(DiscountType.PERCENTAGE, "12.50", 500L, 0, null, true, Set.of());
        assertEquals(500, offer.discount(10_000));
        assertEquals(125, offer.discount(1_000));
    }

    @Test void fixedDiscountIsConvertedFromRupeesToPaiseAndCannotMakeOrderFree() {
        var offer = promotion(DiscountType.FIXED, "150.00", null, 0, null, true, Set.of());
        assertEquals(15000, offer.discount(20_000));
        assertEquals(999, offer.discount(1_000));
    }

    @Test void eligibilityChecksActiveWindowMinimumAndPaymentPurpose() {
        var offer = promotion(DiscountType.PERCENTAGE, "10", null, 5000, null, true,
                Set.of(PaymentPurpose.BOOKING));
        Instant now = Instant.parse("2026-11-15T12:00:00Z");
        assertTrue(offer.eligible(5000, PaymentPurpose.BOOKING, now));
        assertFalse(offer.eligible(4999, PaymentPurpose.BOOKING, now));
        assertFalse(offer.eligible(5000, PaymentPurpose.TRAINING, now));
        assertFalse(offer.eligible(5000, PaymentPurpose.BOOKING, END));
        assertFalse(offer.eligible(5000, PaymentPurpose.BOOKING, START.minusNanos(1)));
    }

    @Test void globalUsageLimitAndInactiveStatePreventFurtherRedemption() {
        var offer = promotion(DiscountType.PERCENTAGE, "10", null, 0, 1, true, Set.of());
        Instant now = Instant.parse("2026-11-15T12:00:00Z");
        assertTrue(offer.eligible(1000, PaymentPurpose.BOOKING, now));
        offer.redeemed();
        assertEquals(1, offer.getUsageCount());
        assertFalse(offer.eligible(1000, PaymentPurpose.BOOKING, now));
        offer.reverseRedemption();
        assertEquals(0, offer.getUsageCount());
        assertTrue(offer.eligible(1000, PaymentPurpose.BOOKING, now));
        offer.update("SAVE", "Save offer", null, DiscountType.PERCENTAGE, new BigDecimal("10"),
                null, 0, Set.of(), false, true, START, END, null, 2, false);
        assertFalse(offer.eligible(1000, PaymentPurpose.BOOKING, now));
    }
}
