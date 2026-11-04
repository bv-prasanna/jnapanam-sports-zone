package com.jnapanam.sportszone.booking.controller;

import com.jnapanam.sportszone.booking.dto.BookingResponse;
import com.jnapanam.sportszone.booking.service.VenueCheckInService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkin/venue")
public class VenueCheckInController {
    private final VenueCheckInService service;

    public VenueCheckInController(VenueCheckInService service) {
        this.service = service;
    }

    @GetMapping("/{venueCode}")
    public List<BookingResponse> eligible(Authentication authentication, @PathVariable String venueCode) {
        return service.eligible(user(authentication), venueCode);
    }

    @PostMapping("/{venueCode}/{bookingNumber}")
    public BookingResponse checkIn(Authentication authentication, @PathVariable String venueCode,
            @PathVariable String bookingNumber) {
        // A public venue-code poster is not proof that the booked customer is on site.
        // Operator scanning now verifies both the signed booking pass and customer ID.
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.GONE,
                "Customer self check-in by venue code is retired. Ask the operator to scan your booking QR.");
    }

    private static UUID user(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
