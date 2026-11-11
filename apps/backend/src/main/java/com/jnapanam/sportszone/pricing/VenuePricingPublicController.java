package com.jnapanam.sportszone.pricing;

import com.jnapanam.sportszone.booking.repository.BookingSlotRepository;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import java.util.UUID;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/pricing")
public class VenuePricingPublicController {
    private final VenuePricingService pricing;
    private final BookingSlotRepository slots;
    public VenuePricingPublicController(VenuePricingService pricing,BookingSlotRepository slots) {
        this.pricing=pricing;this.slots=slots;
    }
    public record BatchQuoteRequest(@NotEmpty @Size(max=100) List<@NotNull UUID> slotIds) {}

    /** One network round-trip for the slot calendar; each quote uses the same backend calculator as checkout. */
    @PostMapping("/slots/quotes")
    public Map<String,VenuePricingService.Preview> quotes(@Valid @RequestBody BatchQuoteRequest request){
        Set<UUID> requested=new LinkedHashSet<>(request.slotIds());
        if(requested.size()!=request.slotIds().size())
            throw new com.jnapanam.sportszone.common.exception.BadRequestException("Duplicate slot IDs");
        Map<UUID,com.jnapanam.sportszone.booking.model.BookingSlot> found=new HashMap<>();
        slots.findAllForPricing(requested).forEach(slot->found.put(slot.getId(),slot));
        if(found.size()!=requested.size())throw new NotFoundException("One or more selected slots are no longer available");
        Map<String,VenuePricingService.Preview> quotes=new LinkedHashMap<>();
        for(UUID id:requested)quotes.put(id.toString(),pricing.quote(found.get(id),1));
        return quotes;
    }

    @GetMapping("/slots/{slotId}")
    public VenuePricingService.Preview slotPrice(@PathVariable UUID slotId,@RequestParam(defaultValue="1") int quantity) {
        return pricing.quote(slots.findForPricingById(slotId).orElseThrow(()->new NotFoundException("Slot not found")),quantity);
    }
}
