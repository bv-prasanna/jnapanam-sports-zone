package com.jnapanam.sportszone.pricing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnapanam.sportszone.booking.model.Booking;
import com.jnapanam.sportszone.booking.repository.BookingRepository;
import com.jnapanam.sportszone.common.exception.ConflictException;
import com.jnapanam.sportszone.payment.model.*;
import com.jnapanam.sportszone.payment.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VenueBillingServiceTest {
    private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    private final PaymentOrderRepository orders=mock(PaymentOrderRepository.class);
    private final BookingRepository bookings=mock(BookingRepository.class);
    private final PaymentRefundRepository refunds=mock(PaymentRefundRepository.class);
    private final VenueBillingService service=new VenueBillingService(jdbc,orders,bookings,refunds,new ObjectMapper());

    @Test void merchantDraftCanBeSavedWithoutGstin(){
        var draft=new VenueBillingService.BillingProfile("Jnapanam Sports Zone",null,
           "", "29",null,null);
        assertDoesNotThrow(()->service.updateProfile(draft));
        verify(jdbc).update(contains("insert into venue_billing_profile"),any(Object[].class));
    }

    @Test void anotherCustomerCannotReadReceiptOrRequestInvoice(){
        UUID paymentId=UUID.randomUUID(),ownerId=UUID.randomUUID(),attackerId=UUID.randomUUID();
        PaymentOrder order=mock(PaymentOrder.class);
        com.jnapanam.sportszone.identity.model.User owner=mock(com.jnapanam.sportszone.identity.model.User.class);
        when(owner.getId()).thenReturn(ownerId);
        when(order.getUser()).thenReturn(owner);
        when(orders.findById(paymentId)).thenReturn(Optional.of(order));
        assertThrows(com.jnapanam.sportszone.common.exception.NotFoundException.class,
            ()->service.receipt(attackerId,paymentId,false));
        assertThrows(com.jnapanam.sportszone.common.exception.NotFoundException.class,
            ()->service.requestOwnInvoice(attackerId,paymentId));
    }

    @Test void invoiceCannotBeIssuedBeforeCapture(){
        UUID id=UUID.randomUUID();
        PaymentOrder order=mock(PaymentOrder.class);
        when(order.getStatus()).thenReturn(PaymentStatus.CREATED);
        when(orders.findById(id)).thenReturn(Optional.of(order));
        assertThrows(ConflictException.class,()->service.issueInvoice(id,
            new VenueBillingService.InvoiceDetails(null,"29","Arena Bengaluru","CGST_SGST")));
        verifyNoInteractions(bookings);
    }

    @Test void invoiceCannotBeIssuedForBookingWithoutTaxSnapshot(){
        UUID id=UUID.randomUUID(),reference=UUID.randomUUID();
        PaymentOrder order=mock(PaymentOrder.class);Booking booking=mock(Booking.class);
        when(order.getStatus()).thenReturn(PaymentStatus.PAID);
        when(order.getPurpose()).thenReturn(PaymentPurpose.BOOKING);
        when(order.getReferenceId()).thenReturn(reference);
        when(orders.findById(id)).thenReturn(Optional.of(order));
        when(bookings.findById(reference)).thenReturn(Optional.of(booking));
        assertThrows(ConflictException.class,()->service.issueInvoice(id,
            new VenueBillingService.InvoiceDetails(null,"29","Arena Bengaluru","IGST")));
    }

    @Test void fullyRefundedPaymentCannotReceiveNewInvoice(){
        UUID id=UUID.randomUUID();
        PaymentOrder order=mock(PaymentOrder.class);
        when(order.getStatus()).thenReturn(PaymentStatus.REFUNDED);
        when(orders.findById(id)).thenReturn(Optional.of(order));
        assertThrows(ConflictException.class,()->service.issueInvoice(id,
            new VenueBillingService.InvoiceDetails(null,"29","Bengaluru","CGST_SGST","29")));
        verifyNoInteractions(bookings);
    }

    @Test void aPendingRefundMustNotProduceCreditNote(){
        UUID id=UUID.randomUUID();
        PaymentRefund refund=mock(PaymentRefund.class);
        when(refund.getStatus()).thenReturn(RefundStatus.REQUESTED);
        when(refunds.findById(id)).thenReturn(Optional.of(refund));
        assertThrows(ConflictException.class,()->service.issueCreditNote(id));
    }
}
