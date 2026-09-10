package com.jnapanam.sportszone.booking.controller;

import com.jnapanam.sportszone.admin.service.OperatorAssignmentService;
import com.jnapanam.sportszone.booking.dto.BookingResponse;
import com.jnapanam.sportszone.booking.service.BookingService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/bookings")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OPERATIONS')")
public class AdminBookingController {
    private final BookingService service;
    private final OperatorAssignmentService assignments;

    public AdminBookingController(BookingService service, OperatorAssignmentService assignments) {
        this.service = service;
        this.assignments = assignments;
    }

    @GetMapping
    public List<BookingResponse> list(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String sport,
            @RequestParam(required = false) UUID facilityId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false, name = "q") String query) {
        List<BookingResponse> values = service.adminList(date, from, to, sport, facilityId, status, paymentStatus, query);
        return assignments.filterBookings(UUID.fromString(authentication.getName()), values);
    }
}
