package com.jnapanam.sportszone.commerce.controller;

import static com.jnapanam.sportszone.commerce.dto.ShopCounterDtos.*;

import com.jnapanam.sportszone.commerce.service.ShopCounterService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shop-counter")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SHOP_KEEPER')")
public class ShopCounterController {
    private final ShopCounterService service;

    public ShopCounterController(ShopCounterService service) {
        this.service = service;
    }

    @PostMapping("/sales")
    @ResponseStatus(HttpStatus.CREATED)
    public WalkInSaleResponse sale(Authentication authentication, @Valid @RequestBody WalkInSaleRequest request) {
        return service.sale(actor(authentication), request);
    }

    @PostMapping("/orders/{id}/collect-payment")
    public Map<String, Object> collect(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody CounterPaymentRequest request) {
        return service.collectOrderPayment(actor(authentication), id, request);
    }

    @PostMapping("/orders/{id}/fulfil")
    public Map<String, Object> fulfil(Authentication authentication, @PathVariable UUID id) {
        return service.fulfil(actor(authentication), id);
    }

    @GetMapping("/returns")
    public List<ReturnResponse> returns() {
        return service.returns();
    }

    @PostMapping("/returns")
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnResponse createReturn(Authentication authentication, @Valid @RequestBody ReturnRequest request) {
        return service.createReturn(actor(authentication), request);
    }

    @PostMapping("/returns/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ReturnResponse approve(Authentication authentication, @PathVariable UUID id) {
        return service.approveRefund(actor(authentication), id);
    }

    @PostMapping("/returns/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ReturnResponse reject(Authentication authentication, @PathVariable UUID id) {
        return service.rejectRefund(actor(authentication), id);
    }

    @PostMapping("/returns/{id}/complete")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ReturnResponse complete(Authentication authentication, @PathVariable UUID id) {
        return service.completeRefund(actor(authentication), id);
    }

    private static UUID actor(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
