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

    @Test void aPendingRefundMustNotProduceCreditNote(){
        UUID id=UUID.randomUUID();
        PaymentRefund refund=mock(PaymentRefund.class);
        when(refund.getStatus()).thenReturn(RefundStatus.REQUESTED);
        when(refunds.findById(id)).thenReturn(Optional.of(refund));
        assertThrows(ConflictException.class,()->service.issueCreditNote(id));
    }
}
