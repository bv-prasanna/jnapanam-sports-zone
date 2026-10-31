package com.jnapanam.sportszone.payment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnapanam.sportszone.booking.repository.BookingRepository;
import com.jnapanam.sportszone.booking.service.BookingService;
import com.jnapanam.sportszone.common.exception.BadRequestException;
import com.jnapanam.sportszone.identity.model.User;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import com.jnapanam.sportszone.loyalty.service.LoyaltyService;
import com.jnapanam.sportszone.membership.repository.CustomerMembershipRepository;
import com.jnapanam.sportszone.notification.service.NotificationService;
import com.jnapanam.sportszone.payment.config.PaymentProperties;
import com.jnapanam.sportszone.payment.dto.RefundRequest;
import com.jnapanam.sportszone.payment.gateway.PaymentGateway;
import com.jnapanam.sportszone.payment.model.*;
import com.jnapanam.sportszone.payment.repository.*;
import com.jnapanam.sportszone.promotion.service.PromotionService;
import com.jnapanam.sportszone.tournament.repository.TournamentTeamRepository;
import com.jnapanam.sportszone.training.repository.TrainingEnrollmentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RazorpayWebhookLifecycleTest {
    final PaymentOrderRepository orders = mock(PaymentOrderRepository.class);
    final PaymentRefundRepository refunds = mock(PaymentRefundRepository.class);
    final PaymentWebhookEventRepository events = mock(PaymentWebhookEventRepository.class);
    final PaymentGateway gateway = mock(PaymentGateway.class);
    final NotificationService notifications = mock(NotificationService.class);
    final BookingRepository bookings = mock(BookingRepository.class);
    final PaymentService service = new PaymentService(orders, refunds,
            mock(UserRepository.class), bookings, mock(CustomerMembershipRepository.class),
            mock(TrainingEnrollmentRepository.class), gateway,
            new PaymentProperties("razorpay","rzp_test_dummy","testsecret","webhooksecret","INR"),
            events, notifications, mock(TournamentTeamRepository.class), mock(BookingService.class),
            mock(PromotionService.class), mock(LoyaltyService.class));
    final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach void acceptedSignatures() {
        when(gateway.verifyWebhook(anyString(), anyString())).thenReturn(true);
        when(gateway.provider()).thenReturn("RAZORPAY");
    }

    void event(String id, String json) throws Exception {
        service.webhook(id,"valid-signature",json,mapper.readTree(json));
    }

    @Test void invalidSignatureRejectedBeforeDuplicateLookup() throws Exception {
        when(gateway.verifyWebhook(anyString(), anyString())).thenReturn(false);
        assertThrows(BadRequestException.class,
            () -> event("evt-fake", "{\"event\":\"payment.failed\"}"));
        verifyNoInteractions(events);
    }

    @Test void duplicateVerifiedEventIgnored() throws Exception {
        when(events.existsByProviderEventId("evt-duplicate")).thenReturn(true);
        event("evt-duplicate","{\"event\":\"payment.failed\"}");
        verify(events,never()).save(any());
    }

    @Test void failedAttemptIsRecordedWithoutCancellingOrder() throws Exception {
        event("evt-failed","{\"event\":\"payment.failed\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_failed_1\",\"order_id\":\"order_1\",\"amount\":10000}}}}");
        ArgumentCaptor<PaymentWebhookEvent> captor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
        verify(events).save(captor.capture());
        assertEquals("payment.failed",captor.getValue().getEventType());
        assertEquals("OBSERVED",captor.getValue().getStatus());
        assertEquals("order_1",captor.getValue().getProviderOrderId());
        verifyNoInteractions(orders);
        verifyNoInteractions(notifications);
    }

    @Test void settlementEventIsAuditedButNeverFulfilsBooking() throws Exception {
        event("evt-settlement","{\"event\":\"settlement.processed\",\"payload\":{\"settlement\":{\"entity\":{\"id\":\"setl_001\",\"amount\":50000}}}}");
        ArgumentCaptor<PaymentWebhookEvent> captor = ArgumentCaptor.forClass(PaymentWebhookEvent.class);
        verify(events).save(captor.capture());
        assertEquals("settlement.processed",captor.getValue().getEventType());
        assertEquals("setl_001",captor.getValue().getProviderObjectId());
        assertEquals("OBSERVED",captor.getValue().getStatus());
        verifyNoInteractions(orders);
    }

    PaymentOrder paidOrder(){
        User user=mock(User.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        PaymentOrder order=new PaymentOrder(user,PaymentPurpose.MEMBERSHIP,UUID.randomUUID(),10000,"INR","RAZORPAY","key");
        order.providerOrder("order_001");
        order.paid("pay_001");
        return order;
    }

    @Test void razorpayRefundCreationRemainsRequestedUntilWebhook() {
        PaymentOrder order=paidOrder();
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));
        when(refunds.findByPaymentOrderId(order.getId())).thenReturn(List.of());
        when(refunds.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(gateway.refund(eq("pay_001"),eq(10000L),anyString()))
            .thenReturn(new PaymentGateway.Refund("rfnd_001"));
        var result=service.refund(order.getId(),new RefundRequest(null,"Customer request"));
        assertEquals(RefundStatus.REQUESTED,result.status());
        assertEquals("rfnd_001",result.providerRefundId());
        assertEquals(PaymentStatus.PAID,order.getStatus());
        verifyNoInteractions(notifications);
    }

    @Test void verifiedRefundProcessedMarksPaymentRefundedOnce() throws Exception {
        PaymentOrder order=paidOrder();
        PaymentRefund refund=new PaymentRefund(order,10000,"Customer request");
        refund.submitted("rfnd_001");
        when(refunds.findByProviderRefundId("rfnd_001")).thenReturn(Optional.of(refund));
        when(refunds.findByPaymentOrderId(order.getId())).thenReturn(List.of(refund));
        String json="{\"event\":\"refund.processed\",\"payload\":{\"refund\":{\"entity\":{\"id\":\"rfnd_001\",\"payment_id\":\"pay_001\",\"amount\":10000}}}}";
        event("evt-refund-processed",json);
        assertEquals(RefundStatus.PROCESSED,refund.getStatus());
        assertEquals(PaymentStatus.REFUNDED,order.getStatus());
        verify(notifications).refund(eq(order.getUser().getId()),eq(order.getReceiptNumber()),eq(10000L));
        event("evt-refund-second-event",json);
        verify(notifications,times(1)).refund(any(),anyString(),anyLong());
    }

    @Test void failedRefundDoesNotMakePaymentRefunded() throws Exception {
        PaymentOrder order=paidOrder();
        PaymentRefund refund=new PaymentRefund(order,10000,"Request");
        refund.submitted("rfnd_001");
        when(refunds.findByProviderRefundId("rfnd_001")).thenReturn(Optional.of(refund));
        event("evt-refund-failed","{\"event\":\"refund.failed\",\"payload\":{\"refund\":{\"entity\":{\"id\":\"rfnd_001\",\"payment_id\":\"pay_001\",\"amount\":10000}}}}");
        assertEquals(RefundStatus.FAILED,refund.getStatus());
        assertEquals(PaymentStatus.PAID,order.getStatus());
        verifyNoInteractions(notifications);
    }

    @Test void mismatchedRefundPaymentIdRejected() throws Exception {
        PaymentOrder order=paidOrder();
        PaymentRefund refund=new PaymentRefund(order,10000,"Request");
        refund.submitted("rfnd_001");
        when(refunds.findByProviderRefundId("rfnd_001")).thenReturn(Optional.of(refund));
        assertThrows(BadRequestException.class,()->event("evt-mismatch","{\"event\":\"refund.processed\",\"payload\":{\"refund\":{\"entity\":{\"id\":\"rfnd_001\",\"payment_id\":\"pay_OTHER\",\"amount\":10000}}}}"));
        assertEquals(RefundStatus.REQUESTED,refund.getStatus());
        verify(events,never()).save(any());
    }
}
