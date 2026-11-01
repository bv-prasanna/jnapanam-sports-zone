package com.jnapanam.sportszone.pricing;

import com.jnapanam.sportszone.booking.repository.BookingSlotRepository;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/pricing")
public class VenuePricingPublicController {
    private final VenuePricingService pricing;
    private final BookingSlotRepository slots;
    public VenuePricingPublicController(VenuePricingService pricing,BookingSlotRepository slots) {
        this.pricing=pricing;this.slots=slots;
    }
    @GetMapping("/slots/{slotId}")
    public VenuePricingService.Preview slotPrice(@PathVariable UUID slotId,@RequestParam(defaultValue="1") int quantity) {
        return pricing.quote(slots.findById(slotId).orElseThrow(()->new NotFoundException("Slot not found")),quantity);
    }
}
