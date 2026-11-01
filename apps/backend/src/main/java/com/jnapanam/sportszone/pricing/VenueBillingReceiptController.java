package com.jnapanam.sportszone.pricing;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class VenueBillingReceiptController {
    private final VenueBillingService service;
    public VenueBillingReceiptController(VenueBillingService service){this.service=service;}
    @GetMapping("/{paymentId}/billing")
    public Map<String,Object> receipt(Authentication authentication,@PathVariable UUID paymentId){
        return service.receipt(UUID.fromString(authentication.getName()),paymentId,false);
    }
}
