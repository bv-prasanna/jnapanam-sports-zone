package com.jnapanam.sportszone.booking.service;

import com.jnapanam.sportszone.booking.dto.*;
import com.jnapanam.sportszone.booking.model.*;
import com.jnapanam.sportszone.booking.repository.*;
import com.jnapanam.sportszone.common.exception.*;
import com.jnapanam.sportszone.identity.model.User;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
    private final BookingSlotRepository slots;
    private final BookingHoldRepository holds;
    private final BookingRepository bookings;
    private final UserRepository users;
    private final long holdMinutes;
    private final long paymentMinutes;

    public BookingService(BookingSlotRepository slots, BookingHoldRepository holds, BookingRepository bookings,
            UserRepository users, @Value("${app.booking.hold-minutes:10}") long holdMinutes,
            @Value("${app.payment.pending-minutes:15}") long paymentMinutes) {
        this.slots = slots;
        this.holds = holds;
        this.bookings = bookings;
        this.users = users;
        this.holdMinutes = holdMinutes;
        this.paymentMinutes = paymentMinutes;
    }

    @Transactional
    public HoldResponse hold(UUID userId, HoldRequest request) {
        cleanupExpired();
        User user = requireUser(userId);
        BookingSlot slot = slots.findLocked(request.slotId())
                .orElseThrow(() -> new NotFoundException("Slot not found"));
        if (slot.getDate().isBefore(LocalDate.now()))
            throw new ConflictException("This slot has already passed");
        slot.hold(request.quantity());
        BookingHold hold = holds
                .save(new BookingHold(user, slot, request.quantity(), Instant.now().plusSeconds(holdMinutes * 60)));
        return HoldResponse.from(hold);
    }

    @Transactional
    public BookingResponse confirm(UUID userId, ConfirmRequest request) {
        BookingHold hold = holds.findLocked(request.holdId())
                .orElseThrow(() -> new NotFoundException("Hold not found"));
        assertOwner(hold.getUser(), userId);
        if (hold.getStatus() != HoldStatus.ACTIVE)
            throw new ConflictException("Hold is no longer active");
        BookingSlot slot = slots.findLocked(hold.getSlot().getId())
                .orElseThrow(() -> new NotFoundException("Slot not found"));
        if (hold.expired(Instant.now())) {
            slot.releaseHold(hold.getQuantity());
            hold.change(HoldStatus.EXPIRED);
            throw new ConflictException("Hold has expired");
        }
        slot.confirm(hold.getQuantity());
        hold.change(HoldStatus.CONVERTED);
        boolean paid = slot.getFacility().getBookingPrice().signum() > 0;
        return BookingResponse.from(bookings.save(new Booking(hold.getUser(), slot, hold.getQuantity(), paid,
                Instant.now().plusSeconds(paymentMinutes * 60))));
    }

    @Transactional
    public void release(UUID userId, UUID holdId) {
        BookingHold hold = holds.findLocked(holdId).orElseThrow(() -> new NotFoundException("Hold not found"));
        assertOwner(hold.getUser(), userId);
        if (hold.getStatus() != HoldStatus.ACTIVE)
            throw new ConflictException("Hold is no longer active");
        BookingSlot slot = slots.findLocked(hold.getSlot().getId()).orElseThrow();
        slot.releaseHold(hold.getQuantity());
        hold.change(HoldStatus.RELEASED);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> mine(UUID userId) {
        return bookings.findByUserIdOrderByConfirmedAtDesc(userId).stream().map(BookingResponse::from).toList();
    }

    @Transactional
    public BookingResponse cancel(UUID userId, UUID bookingId) {
        Booking booking = bookings.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        assertOwner(booking.getUser(), userId);
        return cancelBooking(booking, "Cancelled by customer");
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> adminList(LocalDate date) {
        List<Booking> values = date == null
                ? bookings.findAllByOrderByConfirmedAtDesc()
                : bookings.findBySlot_DateOrderByConfirmedAtDesc(date);

        return values.stream()
                .map(BookingResponse::from)
                .toList();
    }

    @Transactional
    public BookingResponse checkIn(UUID operatorId, String bookingNumber) {
        Booking booking = bookings.findByBookingNumberIgnoreCase(bookingNumber)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
        if (booking.getStatus() != BookingStatus.CONFIRMED)
            throw new ConflictException("Only confirmed bookings can be checked in");
        if (booking.getCheckedInAt() != null)
            throw new ConflictException("Booking is already checked in");
        booking.checkIn(requireUser(operatorId));
        return BookingResponse.from(booking);
    }

    @Transactional
    public BookingResponse adminCancel(UUID id, String reason) {
        return cancelBooking(bookings.findById(id).orElseThrow(() -> new NotFoundException("Booking not found")),
                reason == null || reason.isBlank() ? "Cancelled by operations" : reason.trim());
    }

    @Transactional
    public BookingResponse undoCheckIn(UUID id) {
        Booking b = bookings.findById(id).orElseThrow(() -> new NotFoundException("Booking not found"));
        if (b.getCheckedInAt() == null)
            throw new ConflictException("Booking is not checked in");
        b.undoCheckIn();
        return BookingResponse.from(b);
    }

    @Transactional
    public BookingResponse noShow(UUID id, String reason) {
        Booking b = bookings.findById(id).orElseThrow(() -> new NotFoundException("Booking not found"));
        if (b.getStatus() != BookingStatus.CONFIRMED || b.getCheckedInAt() != null)
            throw new ConflictException("Booking cannot be marked no-show");
        b.noShow(reason == null || reason.isBlank() ? "Marked no-show by operations" : reason.trim());
        return BookingResponse.from(b);
    }

    @Transactional
    public int cleanupUnpaid() {
        int count = 0;
        for (Booking b : bookings.findByStatusAndPaymentExpiresAtLessThanEqual(BookingStatus.PENDING_PAYMENT,
                Instant.now())) {
            BookingSlot slot = slots.findLocked(b.getSlot().getId()).orElseThrow();
            slot.cancel(b.getQuantity());
            b.expirePayment();
            count++;
        }
        return count;
    }

    @Transactional
    public void cleanupExpired() {
        for (BookingHold candidate : holds.findByStatusAndExpiresAtLessThanEqual(HoldStatus.ACTIVE, Instant.now())) {
            BookingHold hold = holds.findLocked(candidate.getId()).orElseThrow();
            if (hold.getStatus() == HoldStatus.ACTIVE && hold.expired(Instant.now())) {
                BookingSlot slot = slots.findLocked(hold.getSlot().getId()).orElseThrow();
                slot.releaseHold(hold.getQuantity());
                hold.change(HoldStatus.EXPIRED);
            }
        }
    }

    private User requireUser(UUID id) {
        return users.findById(id).orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    private BookingResponse cancelBooking(Booking booking, String reason) {
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.PENDING_PAYMENT)
            throw new ConflictException("Booking cannot be cancelled");
        BookingSlot slot = slots.findLocked(booking.getSlot().getId()).orElseThrow();
        if (!LocalDateTime.of(slot.getDate(), slot.getStartTime()).isAfter(LocalDateTime.now()))
            throw new ConflictException("Started bookings cannot be cancelled");
        slot.cancel(booking.getQuantity());
        booking.cancel(reason);
        return BookingResponse.from(booking);
    }

    private static void assertOwner(User user, UUID id) {
        if (!user.getId().equals(id))
            throw new NotFoundException("Resource not found");
    }
}
