package com.jnapanam.sportszone.pricing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class VenueBillingPdfController {
    private final VenueBillingService billing;
    private final VenueInvoicePdfService pdf;
    private final ObjectMapper json;
    public VenueBillingPdfController(VenueBillingService billing,VenueInvoicePdfService pdf,ObjectMapper json){
        this.billing=billing;this.pdf=pdf;this.json=json;
    }
    @GetMapping(value="/{paymentId}/billing/receipt/pdf",produces="application/pdf")
    public ResponseEntity<byte[]> receiptPdf(Authentication authentication,@PathVariable UUID paymentId){
        boolean admin=authentication.getAuthorities().stream()
            .anyMatch(a->Set.of("ROLE_SUPER_ADMIN","ROLE_ADMIN").contains(a.getAuthority()));
        Map<String,Object> receipt=billing.receipt(UUID.fromString(authentication.getName()),paymentId,admin);
        if(!Set.of("PAID","PARTIALLY_REFUNDED","REFUNDED").contains(String.valueOf(receipt.get("status"))))
            throw new com.jnapanam.sportszone.common.exception.ConflictException("Payment receipt is available only after payment confirmation");
        byte[] bytes=pdf.renderPaymentReceipt(receipt);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"jnapanam-payment-receipt.pdf\"")
            .body(bytes);
    }

    @GetMapping(value="/{paymentId}/billing/{number}/pdf",produces="application/pdf")
    public ResponseEntity<byte[]> pdf(Authentication authentication,@PathVariable UUID paymentId,@PathVariable String number){
        boolean admin=authentication.getAuthorities().stream()
                .anyMatch(a->Set.of("ROLE_SUPER_ADMIN","ROLE_ADMIN").contains(a.getAuthority()));
        Map<String,Object> receipt=billing.receipt(UUID.fromString(authentication.getName()),paymentId,admin);
        Object documents=receipt.get("documents");
        if(!(documents instanceof List<?> list))throw new NotFoundException("Invoice not found");
        for(Object v:list){
            if(!(v instanceof Map<?,?> doc)||!number.equals(doc.get("documentNumber")))continue;
            try{
                Map<String,Object> details=json.readValue(String.valueOf(doc.get("details")),new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});
                byte[] bytes=pdf.render(number,String.valueOf(doc.get("kind")),details);
                return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"jnapanam-"+number+".pdf\"").body(bytes);
            }catch(java.io.IOException e){throw new IllegalStateException("Stored invoice document could not be read",e);}
        }
        throw new NotFoundException("Invoice not found");
    }
}
