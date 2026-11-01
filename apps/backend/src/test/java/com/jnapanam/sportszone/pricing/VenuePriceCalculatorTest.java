package com.jnapanam.sportszone.pricing;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.jnapanam.sportszone.pricing.VenuePriceCalculator.*;

class VenuePriceCalculatorTest {
    private static final LocalDate DATE = LocalDate.of(2026,11,2);
    private static final LocalTime TIME = LocalTime.of(18,0);
    private static final BigDecimal BASE = new BigDecimal("500.00");
    private static Quote quote(List<Rule> rules,List<Tax> taxes) {
        return VenuePriceCalculator.calculate(BASE,2,"BADMINTON","COURT_1","SLOT_1",DATE,TIME,rules,taxes);
    }
    @Test void peakHourSurchargeAndExclusiveTax() {
        var peak = new Rule("peak",Scope.FACILITY,"COURT_1",Adjustment.ADD_PERCENT,new BigDecimal("20"),1,
                null,null,LocalTime.of(17,0),LocalTime.of(21,0),Set.of(DayOfWeek.MONDAY));
        var tax = new Tax("tax",Scope.SITE,null,new BigDecimal("18"),false,null,null);
        var result = quote(List.of(peak),List.of(tax));
        assertEquals(new BigDecimal("600.00"),result.unitAfterAdjustment());
        assertEquals(new BigDecimal("216.00"),result.tax());
        assertEquals(new BigDecimal("1416.00"),result.total());
    }
    @Test void slotRuleOverridesEntireSiteAndSport() {
        var global = new Rule("site",Scope.SITE,null,Adjustment.ADD_PERCENT,new BigDecimal("50"),1,null,null,null,null,Set.of());
        var slot = new Rule("offpeak",Scope.SLOT,"SLOT_1",Adjustment.DISCOUNT_AMOUNT,new BigDecimal("100"),1,null,null,null,null,Set.of());
        assertEquals(new BigDecimal("800.00"),quote(List.of(global,slot),List.of()).total());
        assertEquals("offpeak",quote(List.of(global,slot),List.of()).priceRuleId());
    }
    @Test void inclusiveTaxDoesNotIncreaseCustomerTotal() {
        var tax = new Tax("inclusive",Scope.SPORT,"BADMINTON",new BigDecimal("18"),true,null,null);
        var result = quote(List.of(),List.of(tax));
        assertEquals(new BigDecimal("1000.00"),result.total());
        assertEquals(new BigDecimal("847.46"),result.net());
        assertEquals(new BigDecimal("152.54"),result.tax());
    }
    @Test void expiredAndWrongWeekdayRuleIgnored() {
        var bad = new Rule("expired",Scope.SLOT,"SLOT_1",Adjustment.FIXED_PRICE,BigDecimal.ONE,1,
            null,DATE.minusDays(1),null,null,Set.of());
        assertEquals(new BigDecimal("1000.00"),quote(List.of(bad),List.of()).total());
    }
    @Test void priceCannotDropBelowZero() {
        var discount = new Rule("discount",Scope.SITE,null,Adjustment.DISCOUNT_AMOUNT,new BigDecimal("900"),1,null,null,null,null,Set.of());
        assertEquals(new BigDecimal("0.00"),quote(List.of(discount),List.of()).total());
    }
    @Test void invalidQuantityRejected() {
        assertThrows(IllegalArgumentException.class,()->VenuePriceCalculator.calculate(BASE,0,"A","B","C",DATE,TIME,List.of(),List.of()));
    }
}
