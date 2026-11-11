package com.jnapanam.sportszone.pricing;

import com.jnapanam.sportszone.booking.model.Booking;
import com.jnapanam.sportszone.booking.repository.BookingRepository;
import com.jnapanam.sportszone.payment.model.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentTaxSnapshotTest {
    private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    private final BookingRepository bookings=mock(BookingRepository.class);
    private final PaymentTaxSnapshotService service=new PaymentTaxSnapshotService(jdbc,bookings);

    private PaymentOrder newOrder(long original,long discount,long points,long pointsValue) {
        PaymentOrder o=new PaymentOrder(null,PaymentPurpose.MEMBERSHIP,UUID.randomUUID(),original,"INR","MOCK","key");
        o.pricing(original,discount,"WELCOME",points,pointsValue);
        when(jdbc.query(anyString(),org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<BigDecimal>>any(),eq("MEMBERSHIP")))
            .thenReturn(List.of(new BigDecimal("18")));
        return o;
    }

    @Test void couponAndLoyaltyTenderHaveDistinctAccounting() {
        PaymentOrder o=newOrder(11800,1180,2,200);
        when(jdbc.queryForList(contains("coupon_tax_treatment"))).thenReturn(List.of(Map.of(
            "couponTaxTreatment","INVOICE_DISCOUNT","loyaltyTaxTreatment","PAYMENT_TENDER")));
        service.snapshot(o);
        assertEquals(10620L,o.getBillingGrossPaise());
        assertEquals(1620L,o.getBillingTaxPaise());
        assertEquals(9000L,o.getBillingTaxablePaise());
        assertEquals(200L,o.getBillingLoyaltyTenderPaise());
        assertEquals(10420L,o.getAmountPaise());
    }

    @Test void unreviewedCouponBlocksInvoiceSnapshot() {
        PaymentOrder o=newOrder(11800,1180,0,0);
        when(jdbc.queryForList(contains("coupon_tax_treatment"))).thenReturn(List.of(Map.of(
            "couponTaxTreatment","REVIEW_REQUIRED","loyaltyTaxTreatment","REVIEW_REQUIRED")));
        service.snapshot(o);
        assertNull(o.getBillingGrossPaise());
    }

    @Test void loyaltyDiscountReducesTaxableValueWhenPolicyExplicit() {
        PaymentOrder o=newOrder(11800,0,1,1180);
        when(jdbc.queryForList(contains("coupon_tax_treatment"))).thenReturn(List.of(Map.of(
            "couponTaxTreatment","REVIEW_REQUIRED","loyaltyTaxTreatment","INVOICE_DISCOUNT")));
        service.snapshot(o);
        assertEquals(10620L,o.getBillingGrossPaise());
        assertEquals(9000L,o.getBillingTaxablePaise());
        assertEquals(1620L,o.getBillingTaxPaise());
        assertEquals(0L,o.getBillingLoyaltyTenderPaise());
    }
}
