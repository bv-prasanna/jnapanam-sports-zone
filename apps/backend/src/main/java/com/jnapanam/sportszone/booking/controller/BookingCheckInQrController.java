package com.jnapanam.sportszone.booking.controller;

import com.jnapanam.sportszone.booking.dto.BookingResponse;
import com.jnapanam.sportszone.booking.service.BookingCheckInQrService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class BookingCheckInQrController {
    private final BookingCheckInQrService passes;
    public BookingCheckInQrController(BookingCheckInQrService passes){this.passes=passes;}

    public record ScanRequest(@NotBlank String value) {}
    public record CompleteRequest(@NotBlank String value,boolean identityVerified) {}

    /** Customer obtains the same QR data on every opening until reschedule or check-in. */
    @GetMapping("/api/v1/bookings/{id}/checkin-pass")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','SUPER_ADMIN')")
    public BookingCheckInQrService.Pass customerPass(Authentication auth,@PathVariable UUID id){
        return passes.pass(UUID.fromString(auth.getName()),id);
    }

    /** Never expose a signed pass in a GET query string, provider referrer or access log. */
    @PostMapping("/api/v1/operations/check-in/qr/verify")
    @PreAuthorize("hasAnyRole('OPERATIONS','ADMIN','SUPER_ADMIN')")
    public BookingCheckInQrService.Scan verify(Authentication auth,@Valid @RequestBody ScanRequest request) {
        return passes.verify(UUID.fromString(auth.getName()),request.value());
    }

    @PostMapping("/api/v1/operations/check-in/qr/confirm")
    @PreAuthorize("hasAnyRole('OPERATIONS','ADMIN','SUPER_ADMIN')")
    public BookingResponse checkIn(Authentication auth,@Valid @RequestBody CompleteRequest request) {
        return passes.checkIn(UUID.fromString(auth.getName()),request.value(),request.identityVerified());
    }
}
