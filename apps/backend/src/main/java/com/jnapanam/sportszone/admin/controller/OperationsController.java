package com.jnapanam.sportszone.admin.controller;

import com.jnapanam.sportszone.admin.dto.OperationalReasonRequest;
import com.jnapanam.sportszone.admin.service.OperatorAssignmentService;
import com.jnapanam.sportszone.booking.dto.BookingCheckInRequest;
import com.jnapanam.sportszone.booking.dto.BookingResponse;
import com.jnapanam.sportszone.booking.service.BookingService;
import com.jnapanam.sportszone.payment.dto.BookingCancellationRefundResponse;
import com.jnapanam.sportszone.payment.dto.OnsitePaymentRequest;
import com.jnapanam.sportszone.payment.dto.PaymentResponse;
import com.jnapanam.sportszone.payment.service.PaymentService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operations")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OPERATIONS')")
public class OperationsController {
    private final BookingService bookings;
    private final PaymentService payments;
    private final OperatorAssignmentService assignments;

    public OperationsController(BookingService bookings, PaymentService payments,
            OperatorAssignmentService assignments) {
        this.bookings = bookings;
        this.payments = payments;
        this.assignments = assignments;
    }

    @PostMapping("/bookings/{bookingNumber}/check-in")
    public BookingResponse checkIn(Authentication auth, @PathVariable String bookingNumber,
            @Valid @RequestBody(required = false) BookingCheckInRequest request) {
        UUID actor = UUID.fromString(auth.getName());
        assignments.assertBookingAllowed(actor, bookingNumber);
        return bookings.checkIn(actor, bookingNumber, request);
    }

    @PostMapping("/bookings/{id}/check-out")
    public BookingResponse checkOut(Authentication auth, @PathVariable UUID id) {
        UUID actor = UUID.fromString(auth.getName());
        assignments.assertBookingAllowed(actor, id);
        return bookings.checkOut(actor, id);
    }

    @PostMapping("/bookings/{id}/check-in/reverse")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public BookingResponse reverse(@PathVariable UUID id) {
        return bookings.undoCheckIn(id);
    }

    @PostMapping("/bookings/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public BookingResponse cancel(@PathVariable UUID id,
            @Valid @RequestBody(required = false) OperationalReasonRequest request) {
        return bookings.adminCancel(id, request == null ? null : request.reason());
    }

    @PostMapping("/bookings/{id}/no-show")
    public BookingResponse noShow(Authentication auth, @PathVariable UUID id,
            @Valid @RequestBody(required = false) OperationalReasonRequest request) {
        UUID actor = UUID.fromString(auth.getName());
        assignments.assertBookingAllowed(actor, id);
        return bookings.noShow(id, request == null ? null : request.reason());
    }

    @PostMapping("/bookings/{id}/cancel-and-refund")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public BookingCancellationRefundResponse cancelAndRefund(@PathVariable UUID id,
            @Valid @RequestBody(required = false) OperationalReasonRequest request) {
        return payments.cancelBookingAndRefund(id, request == null ? null : request.reason());
    }

    @PostMapping("/bookings/{id}/collect-payment")
    public PaymentResponse collectPayment(Authentication auth, @PathVariable UUID id,
            @Valid @RequestBody OnsitePaymentRequest request) {
        UUID actor = UUID.fromString(auth.getName());
        assignments.assertBookingAllowed(actor, id);
        return payments.collectOnsite(actor, id, request);
    }
}
