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
    private final PaymentTaxSnapshotService tax;
    public VenueBillingAdminController(VenueBillingService service,PaymentTaxSnapshotService tax){this.service=service;this.tax=tax;}

    public record PurposeTaxRequest(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.DecimalMin("0.0") @jakarta.validation.constraints.DecimalMax("100.0") java.math.BigDecimal taxRatePercent,boolean active,String placeOfSupplyStateCode,boolean autoIssueEnabled){}
    @GetMapping("/purpose-taxes") public java.util.List<java.util.Map<String,Object>> purposes(){return tax.policies();}
    @PutMapping("/purpose-taxes/{purpose}") public java.util.Map<String,String> purpose(@PathVariable String purpose,@Valid @RequestBody PurposeTaxRequest request){
        tax.configure(purpose,request.taxRatePercent(),request.active(),request.placeOfSupplyStateCode(),request.autoIssueEnabled());return java.util.Map.of("status","SAVED");
    }
    @GetMapping("/tax-treatments") public java.util.Map<String,Object> treatments(){return tax.treatmentSettings();}
    @PutMapping("/tax-treatments") public java.util.Map<String,String> treatments(@Valid @RequestBody PaymentTaxSnapshotService.Treatments request){
        tax.configureTreatments(request);return java.util.Map.of("status","SAVED");
    }
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
