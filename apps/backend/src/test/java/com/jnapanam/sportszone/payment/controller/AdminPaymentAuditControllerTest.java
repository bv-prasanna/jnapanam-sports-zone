package com.jnapanam.sportszone.payment.controller;

import com.jnapanam.sportszone.payment.model.PaymentWebhookEvent;
import com.jnapanam.sportszone.payment.repository.PaymentWebhookEventRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Only safe provider metadata is returned to Admin. Never leak webhook payloads. */
class AdminPaymentAuditControllerTest {
    @Test void latestEventsContainGatewayIdentifiersWithoutPayloadOrSecrets(){
        var repo=mock(PaymentWebhookEventRepository.class);
        var event=new PaymentWebhookEvent("evt_123","payment.failed",
            "a".repeat(64),"OBSERVED","pay_123","order_123","pay_123",15000L);
        when(repo.findAllByOrderByProcessedAtDesc(any(Pageable.class))).thenReturn(List.of(event));
        var rows=new AdminPaymentAuditController(repo).recent();
        assertEquals(1,rows.size());
        assertEquals("payment.failed",rows.get(0).type());
        assertEquals("OBSERVED",rows.get(0).result());
        assertEquals("order_123",rows.get(0).providerOrderId());
        assertEquals(15000L,rows.get(0).amountPaise());
        var page=ArgumentCaptor.forClass(Pageable.class);
        verify(repo).findAllByOrderByProcessedAtDesc(page.capture());
        assertEquals(100,page.getValue().getPageSize());
    }
}
