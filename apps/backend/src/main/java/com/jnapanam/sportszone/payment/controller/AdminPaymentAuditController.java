package com.jnapanam.sportszone.payment.controller;

import com.jnapanam.sportszone.payment.repository.PaymentWebhookEventRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Gateway event metadata for payment reconciliation. No card, UPI or payload secrets exposed. */
@RestController
@RequestMapping("/api/v1/admin/payments")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class AdminPaymentAuditController {
    private final PaymentWebhookEventRepository events;
    public AdminPaymentAuditController(PaymentWebhookEventRepository events){this.events=events;}

    public record EventView(String type,String result,String providerOrderId,String providerPaymentId,
                            Long amountPaise,Instant receivedAt){}
    @GetMapping("/webhook-events")
    public List<EventView> recent(){
        return events.findAllByOrderByProcessedAtDesc(PageRequest.of(0,100)).stream()
            .map(e->new EventView(e.getEventType(),e.getStatus(),e.getProviderOrderId(),
                e.getProviderPaymentId(),e.getAmountPaise(),e.getProcessedAt())).toList();
    }
}
