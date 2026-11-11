package com.jnapanam.sportszone.pricing;

import com.jnapanam.sportszone.booking.model.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.jnapanam.sportszone.pricing.VenuePriceCalculator.*;

class VenuePriceInvariantsTest {
    @Test void aBookingCannotHaveItsPriceOverwritten(){
        Booking booking=new Booking(null,null,1,true,PaymentOption.PAY_NOW,true,Instant.now().plusSeconds(900));
        booking.lockPrice(50000,60000,10800,70800,"peak", "gst");
        assertEquals(70800L,booking.getPriceTotalPaise());
        assertEquals(10800L,booking.getPriceTaxPaise());
        assertThrows(IllegalStateException.class,()->booking.lockPrice(50000,40000,0,40000,"discount",null));
    }
    @Test void rejectsPriceThatDoesNotReconcile(){
        Booking booking=new Booking(null,null,1,true,PaymentOption.PAY_NOW,true,Instant.now().plusSeconds(900));
        assertThrows(IllegalArgumentException.class,()->booking.lockPrice(50000,40000,10800,40800,null,null));
    }
    @Test void moreSpecificTaxesOverrideSiteRules(){
        var generic=new Tax("site",Scope.SITE,null,new BigDecimal("18"),false,null,null);
        var facility=new Tax("facility",Scope.FACILITY,"F1",new BigDecimal("5"),false,null,null);
        var result=VenuePriceCalculator.calculate(new BigDecimal("500"),1,"BADMINTON","F1","S1",
          LocalDate.of(2026,11,2),LocalTime.NOON,List.of(),List.of(generic,facility));
        assertEquals(new BigDecimal("525.00"),result.total());
        assertEquals("facility",result.taxRuleId());
    }
    @Test void weekdayAndTimeWindowsDoNotLeakIntoOtherSlots(){
        var peak=new Rule("peak",Scope.SITE,null,Adjustment.ADD_PERCENT,new BigDecimal("20"),1,null,null,
              LocalTime.of(18,0),LocalTime.of(21,0),Set.of(DayOfWeek.MONDAY));
        var result=VenuePriceCalculator.calculate(new BigDecimal("500"),1,"BADMINTON","F1","S1",
          LocalDate.of(2026,11,2),LocalTime.of(14,0),List.of(peak),List.of());
        assertEquals(new BigDecimal("500.00"),result.total());
    }
}
