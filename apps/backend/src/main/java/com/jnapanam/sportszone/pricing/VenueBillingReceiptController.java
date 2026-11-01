package com.jnapanam.sportszone.pricing;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class VenueBillingReceiptController {
    private final VenueBillingService service;
    public VenueBillingReceiptController(VenueBillingService service){this.service=service;}
    @PostMapping("/{paymentId}/billing/invoice")
    public Map<String,Object> requestInvoice(Authentication authentication,@PathVariable UUID paymentId){
        return service.requestOwnInvoice(UUID.fromString(authentication.getName()),paymentId);
    }

    @GetMapping("/{paymentId}/billing")
    public Map<String,Object> receipt(Authentication authentication,@PathVariable UUID paymentId){
        boolean admin=authentication.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN")||a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        return service.receipt(UUID.fromString(authentication.getName()),paymentId,admin);
    }
}
