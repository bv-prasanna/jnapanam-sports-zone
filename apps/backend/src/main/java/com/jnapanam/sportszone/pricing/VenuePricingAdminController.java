package com.jnapanam.sportszone.pricing;

import com.jnapanam.sportszone.booking.repository.BookingSlotRepository;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class VenuePricingAdminController {
    private final VenuePricingService pricing;
    public VenuePricingAdminController(VenuePricingService pricing){this.pricing=pricing;}

    @GetMapping("/pricing-rules") public List<Map<String,Object>> prices(){return pricing.prices();}
    @PostMapping("/pricing-rules") public ResponseEntity<Map<String,UUID>> addPrice(@Valid @RequestBody VenuePricingService.PriceRuleInput r){
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id",pricing.savePrice(null,r)));
    }
    @PutMapping("/pricing-rules/{id}") public Map<String,UUID> changePrice(@PathVariable UUID id,@Valid @RequestBody VenuePricingService.PriceRuleInput r){
        return Map.of("id",pricing.savePrice(id,r));
    }
    @DeleteMapping("/pricing-rules/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void disablePrice(@PathVariable UUID id){
        pricing.deactivate("venue_price_rules",id);
    }

    @GetMapping("/taxes") public List<Map<String,Object>> taxes(){return pricing.taxes();}
    @PostMapping("/taxes") public ResponseEntity<Map<String,UUID>> addTax(@Valid @RequestBody VenuePricingService.TaxRuleInput r){
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id",pricing.saveTax(null,r)));
    }
    @PutMapping("/taxes/{id}") public Map<String,UUID> changeTax(@PathVariable UUID id,@Valid @RequestBody VenuePricingService.TaxRuleInput r){
        return Map.of("id",pricing.saveTax(id,r));
    }
    @DeleteMapping("/taxes/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void disableTax(@PathVariable UUID id){
        pricing.deactivate("venue_tax_rules",id);
    }
}
