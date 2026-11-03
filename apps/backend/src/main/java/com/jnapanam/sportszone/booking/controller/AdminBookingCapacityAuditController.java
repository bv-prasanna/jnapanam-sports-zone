package com.jnapanam.sportszone.booking.controller;

import com.jnapanam.sportszone.booking.service.BookingCapacityAuditService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Staff-only, read-only diagnostics; payment capture is never inferred from slot occupancy. */
@RestController
@RequestMapping("/api/v1/admin/bookings/capacity-audit")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
public class AdminBookingCapacityAuditController {
    private final BookingCapacityAuditService service;
    public AdminBookingCapacityAuditController(BookingCapacityAuditService service) {this.service=service;}

    @GetMapping
    public List<BookingCapacityAuditService.SlotAudit> audit(
            @RequestParam UUID facilityId,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.inspect(facilityId,date);
    }
}
