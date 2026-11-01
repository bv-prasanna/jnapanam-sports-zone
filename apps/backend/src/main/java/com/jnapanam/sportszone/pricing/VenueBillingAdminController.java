package com.jnapanam.sportszone.pricing;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/billing")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class VenueBillingAdminController {
    private final VenueBillingService service;
    public VenueBillingAdminController(VenueBillingService service){this.service=service;}

    @GetMapping("/profile") public Map<String,Object> profile(){return service.profile();}
    @PutMapping("/profile") public Map<String,String> profile(@Valid @RequestBody VenueBillingService.BillingProfile r){
        service.updateProfile(r);return Map.of("status","SAVED");
    }
    @GetMapping("/payments/{paymentId}") public Map<String,Object> review(@PathVariable UUID paymentId){
        return service.receipt(null,paymentId,true);
    }
    @PostMapping("/invoices/{paymentId}/issue")
    public Map<String,Object> issue(@PathVariable UUID paymentId,@RequestBody VenueBillingService.InvoiceDetails r){
        return service.issueInvoice(paymentId,r);
    }
    @PostMapping("/refunds/{refundId}/credit-note")
    public Map<String,Object> creditNote(@PathVariable UUID refundId){
        return service.issueCreditNote(refundId);
    }
}
