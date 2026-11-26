package com.jnapanam.sportszone.release1.controller;

import com.jnapanam.sportszone.admin.service.OperatorAssignmentService;
import com.jnapanam.sportszone.booking.dto.BookingResponse;
import com.jnapanam.sportszone.booking.dto.ConfirmRequest;
import com.jnapanam.sportszone.booking.dto.HoldRequest;
import com.jnapanam.sportszone.booking.dto.HoldResponse;
import com.jnapanam.sportszone.booking.service.BookingService;
import com.jnapanam.sportszone.booking.model.PaymentOption;
import com.jnapanam.sportszone.notification.service.NotificationService;
import com.jnapanam.sportszone.payment.service.PaymentService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/frontdesk")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OPERATIONS')")
public class FrontDeskController {
    private final BookingService bookings;
    private final JdbcTemplate jdbc;
    private final NotificationService notifications;
    private final PaymentService payments;
    private final OperatorAssignmentService assignments;

    public FrontDeskController(BookingService bookings, JdbcTemplate jdbc, NotificationService notifications,
            PaymentService payments, OperatorAssignmentService assignments) {
        this.bookings = bookings;
        this.jdbc = jdbc;
        this.notifications = notifications;
        this.payments = payments;
        this.assignments = assignments;
    }

    @PostMapping("/walk-in")
    public BookingResponse walkIn(Authentication authentication, @RequestBody Map<String, Object> body) {
        UUID actorId = UUID.fromString(authentication.getName());
        UUID customerId = UUID.fromString(body.get("customerUserId").toString());
        UUID slotId = UUID.fromString(body.get("slotId").toString());
        int quantity = Integer.parseInt(String.valueOf(body.getOrDefault("quantity", 1)));
        assignments.assertSlotAllowed(actorId, slotId);
        HoldResponse hold = bookings.hold(customerId, new HoldRequest(slotId, quantity));
        return bookings.confirm(customerId, new ConfirmRequest(hold.id(), PaymentOption.PAY_AT_VENUE));
    }

    @PostMapping("/facilities/{facilityId}/emergency-close")
    public Map<String, Object> close(Authentication authentication, @PathVariable UUID facilityId,
            @RequestBody Map<String, Object> body) {
        UUID actorId = UUID.fromString(authentication.getName());
        assignments.assertFacilityAllowed(actorId, facilityId);
        LocalDate date = LocalDate.parse(body.get("date").toString());
        String reason = String.valueOf(body.getOrDefault("reason", "Emergency facility closure"));
        List<Map<String, Object>> affected = jdbc.queryForList(
                "select b.id,b.user_id from bookings b join booking_slots s on s.id=b.slot_id "
                        + "where s.facility_id=? and s.slot_date=? and b.status in ('CONFIRMED','PENDING_PAYMENT')",
                facilityId, date);
        int cancelled = 0;
        for (Map<String, Object> booking : affected) {
            UUID bookingId = (UUID) booking.get("id");
            UUID customerId = (UUID) booking.get("user_id");
            try {
                bookings.adminCancel(bookingId, reason);
                try {
                    payments.cancelBookingAndRefund(bookingId, reason);
                } catch (Exception ignored) {
                    // Cancellation is retained; payment/refund exceptions are surfaced by payment workflows.
                }
                notifications.notify(customerId, "FACILITY_CHANGE", "Facility closure affects your booking",
                        reason + ". Your booking was cancelled; any eligible paid amount is being refunded. "
                                + "You may select another available slot.");
                cancelled++;
            } catch (Exception ignored) {
                // Continue processing remaining bookings; the result reports partial completion.
            }
        }
        jdbc.update("insert into facility_emergency_actions(id,facility_id,closure_date,reason,action,affected_bookings,created_by) "
                        + "values(?,?,?,?,?,?,?)",
                UUID.randomUUID(), facilityId, date, reason, "CANCEL_NOTIFY", cancelled, actorId);
        return Map.of("affectedBookings", affected.size(), "cancelledAndNotified", cancelled,
                "date", date, "reason", reason);
    }
}
