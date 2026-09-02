package com.jnapanam.sportszone.payment.controller;
import com.jnapanam.sportszone.payment.dto.*;
import com.jnapanam.sportszone.payment.service.PaymentService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/payments")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class AdminPaymentController {
    private final PaymentService service;
    public AdminPaymentController(PaymentService service) { this.service = service; }

    @GetMapping
    public List<PaymentResponse> all() { return service.all(); }

    @PostMapping("/{id}/refunds")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public RefundResponse refund(@PathVariable UUID id, @Valid @RequestBody RefundRequest request) {
        return service.refund(id, request);
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PaymentResponse voidPayment(@PathVariable UUID id, @Valid @RequestBody VoidPaymentRequest request) {
        return service.voidPayment(id, request);
    }
}
