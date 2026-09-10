package com.jnapanam.sportszone.booking.service;

import com.jnapanam.sportszone.booking.dto.BookingResponse;
import com.jnapanam.sportszone.booking.model.Booking;
import com.jnapanam.sportszone.booking.model.BookingPaymentStatus;
import com.jnapanam.sportszone.booking.model.BookingStatus;
import com.jnapanam.sportszone.booking.repository.BookingRepository;
import com.jnapanam.sportszone.common.exception.ConflictException;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import com.jnapanam.sportszone.identity.model.User;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VenueCheckInService {
    public static final String VENUE_CODE = "JNAPANAM";
    private static final int EARLY_MINUTES = 30;

    private final BookingRepository bookings;
    private final UserRepository users;

    public VenueCheckInService(BookingRepository bookings, UserRepository users) {
        this.bookings = bookings;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> eligible(UUID userId, String venueCode) {
        validateVenue(venueCode);
        return bookings.findByUserIdOrderByConfirmedAtDesc(userId).stream()
                .filter(b -> b.getSlot().getDate().equals(LocalDate.now()))
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .filter(b -> b.getCheckedInAt() == null)
                .map(BookingResponse::from).toList();
    }

    @Transactional
    public BookingResponse checkIn(UUID userId, String venueCode, String bookingNumber) {
        validateVenue(venueCode);
        Booking booking = bookings.findByBookingNumberIgnoreCase(bookingNumber)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
        if (!booking.getUser().getId().equals(userId)) throw new NotFoundException("Booking not found");
        if (!booking.getSlot().getDate().equals(LocalDate.now())) throw new ConflictException("Only today's booking can be checked in");
        if (booking.getStatus() != BookingStatus.CONFIRMED) throw new ConflictException("Only confirmed bookings can be checked in");
        if (booking.getPaymentStatus() == BookingPaymentStatus.UNPAID) throw new ConflictException("Payment due — contact the operator before check-in");
        if (booking.getCheckedInAt() != null) throw new ConflictException("Booking is already checked in");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime opens = LocalDateTime.of(booking.getSlot().getDate(), booking.getSlot().getStartTime()).minusMinutes(EARLY_MINUTES);
        LocalDateTime closes = LocalDateTime.of(booking.getSlot().getDate(), booking.getSlot().getEndTime());
        if (now.isBefore(opens)) throw new ConflictException("QR check-in opens 30 minutes before the slot");
        if (now.isAfter(closes)) throw new ConflictException("This slot's check-in window has closed");
        User customer = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        booking.checkIn(customer, false, "Venue QR self check-in");
        return BookingResponse.from(booking);
    }

    private static void validateVenue(String venueCode) {
        if (venueCode == null || !VENUE_CODE.equalsIgnoreCase(venueCode.trim())) {
            throw new NotFoundException("Venue QR is invalid");
        }
    }
}
