package com.jnapanam.sportszone.pricing;

import com.jnapanam.sportszone.booking.model.BookingSlot;
import com.jnapanam.sportszone.booking.repository.BookingSlotRepository;
import com.jnapanam.sportszone.common.exception.BadRequestException;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VenuePricingBatchQuoteTest {
    private final BookingSlotRepository repository=mock(BookingSlotRepository.class);
    private final VenuePricingService prices=mock(VenuePricingService.class);
    private final VenuePricingPublicController controller=new VenuePricingPublicController(prices,repository);
    @Test void oneBatchReturnsCorrectQuoteForEveryRequestedSlot(){
        UUID first=UUID.randomUUID(),second=UUID.randomUUID();
        BookingSlot a=mock(BookingSlot.class),b=mock(BookingSlot.class);
        when(a.getId()).thenReturn(first);when(b.getId()).thenReturn(second);
        when(repository.findAllForPricing(any())).thenReturn(List.of(a,b));
        var resultA=new VenuePricingService.Preview(new BigDecimal("500.00"),new BigDecimal("600.00"),
            new BigDecimal("600.00"),new BigDecimal("108.00"),new BigDecimal("708.00"),"peak","gst",1,new BigDecimal("18"),false);
        var resultB=new VenuePricingService.Preview(new BigDecimal("500.00"),new BigDecimal("400.00"),
            new BigDecimal("400.00"),new BigDecimal("72.00"),new BigDecimal("472.00"),"offpeak","gst",1,new BigDecimal("18"),false);
        when(prices.quote(a,1)).thenReturn(resultA);when(prices.quote(b,1)).thenReturn(resultB);
        var result=controller.quotes(new VenuePricingPublicController.BatchQuoteRequest(List.of(first,second)));
        assertEquals(new BigDecimal("708.00"),result.get(first.toString()).total());
        assertEquals(new BigDecimal("472.00"),result.get(second.toString()).total());
        verify(repository,times(1)).findAllForPricing(any());
    }
    @Test void singleQuoteUsesEagerFacilityAndSportLookup(){
        UUID id=UUID.randomUUID();
        BookingSlot slot=mock(BookingSlot.class);
        when(repository.findForPricingById(id)).thenReturn(Optional.of(slot));
        when(prices.quote(slot,2)).thenReturn(new VenuePricingService.Preview(
            BigDecimal.TEN,BigDecimal.TEN,BigDecimal.valueOf(20),BigDecimal.ZERO,
            BigDecimal.valueOf(20),null,null,2,BigDecimal.ZERO,false));
        assertEquals(BigDecimal.valueOf(20),controller.slotPrice(id,2).total());
        verify(repository).findForPricingById(id);
        verify(repository,never()).findById(any());
    }
    @Test void batchRejectsDuplicateSlotReferences(){
        UUID first=UUID.randomUUID();
        assertThrows(BadRequestException.class,()->controller.quotes(
            new VenuePricingPublicController.BatchQuoteRequest(List.of(first,first))));
        verifyNoInteractions(repository);
    }
    @Test void batchRejectsUnknownSlotsRatherThanDisplayingPartialPrices(){
        UUID id=UUID.randomUUID();
        when(repository.findAllForPricing(any())).thenReturn(List.of());
        assertThrows(NotFoundException.class,()->controller.quotes(
            new VenuePricingPublicController.BatchQuoteRequest(List.of(id))));
    }
}
