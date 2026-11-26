package com.jnapanam.sportszone.booking.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.jnapanam.sportszone.booking.model.Booking;
import com.jnapanam.sportszone.booking.model.BookingPaymentStatus;
import com.jnapanam.sportszone.booking.model.BookingSlot;
import com.jnapanam.sportszone.booking.model.BookingStatus;
import com.jnapanam.sportszone.booking.repository.BookingRepository;
import com.jnapanam.sportszone.common.exception.ConflictException;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import com.jnapanam.sportszone.identity.model.User;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VenueCheckInServiceTest {
    private static final ZoneId VENUE_ZONE = ZoneId.of("Asia/Kolkata");

    private final BookingRepository bookings = mock(BookingRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final VenueCheckInService service = new VenueCheckInService(bookings, users);
    private final UUID customerId = UUID.randomUUID();
    private final UUID bookingOwnerId = UUID.randomUUID();
    private final Booking booking = mock(Booking.class);
    private final BookingSlot slot = mock(BookingSlot.class);
    private final User customer = mock(User.class);

    @BeforeEach
    void setUp() {
        when(booking.getUser()).thenReturn(customer);
        when(customer.getId()).thenReturn(bookingOwnerId);
        when(booking.getSlot()).thenReturn(slot);
        when(slot.getDate()).thenReturn(LocalDate.now(VENUE_ZONE));
        when(booking.getStatus()).thenReturn(BookingStatus.CONFIRMED);
        when(booking.getPaymentStatus()).thenReturn(BookingPaymentStatus.PAID);
        when(booking.getCheckedInAt()).thenReturn(null);
        when(bookings.findLockedByBookingNumber("JNZ-TEST1234")).thenReturn(Optional.of(booking));
    }

    @Test
    void invalidVenueCodeIsRejectedBeforeBookingLookup() {
        assertThrows(NotFoundException.class,
                () -> service.checkIn(customerId, "OTHER-VENUE", "JNZ-TEST1234"));
        verifyNoInteractions(bookings);
    }

    @Test
    void bookingLookupUsesPessimisticLockAndDoesNotRevealOtherCustomersBooking() {
        assertThrows(NotFoundException.class,
                () -> service.checkIn(customerId, VenueCheckInService.VENUE_CODE, "JNZ-TEST1234"));
        verify(bookings).findLockedByBookingNumber("JNZ-TEST1234");
        verifyNoInteractions(users);
    }

    @Test
    void unpaidBookingCannotBeCheckedIn() {
        when(customer.getId()).thenReturn(customerId);
        when(booking.getPaymentStatus()).thenReturn(BookingPaymentStatus.UNPAID);

        assertThrows(ConflictException.class,
                () -> service.checkIn(customerId, VenueCheckInService.VENUE_CODE, "JNZ-TEST1234"));
        verify(bookings).findLockedByBookingNumber("JNZ-TEST1234");
        verifyNoInteractions(users);
    }

    @Test
    void nullPaymentStateCannotBeCheckedIn() {
        when(customer.getId()).thenReturn(customerId);
        when(booking.getPaymentStatus()).thenReturn(null);

        assertThrows(ConflictException.class,
                () -> service.checkIn(customerId, VenueCheckInService.VENUE_CODE, "JNZ-TEST1234"));
        verifyNoInteractions(users);
    }

    @Test
    void nonConfirmedBookingCannotBeCheckedIn() {
        when(customer.getId()).thenReturn(customerId);
        when(booking.getStatus()).thenReturn(BookingStatus.CANCELLED);

        assertThrows(ConflictException.class,
                () -> service.checkIn(customerId, VenueCheckInService.VENUE_CODE, "JNZ-TEST1234"));
        verifyNoInteractions(users);
    }

    @Test
    void alreadyCheckedInBookingCannotBeCheckedInAgain() {
        when(customer.getId()).thenReturn(customerId);
        when(booking.getCheckedInAt()).thenReturn(Instant.now());

        assertThrows(ConflictException.class,
                () -> service.checkIn(customerId, VenueCheckInService.VENUE_CODE, "JNZ-TEST1234"));
        verifyNoInteractions(users);
    }

    @Test
    void bookingFromAnotherVenueDateCannotBeCheckedIn() {
        when(customer.getId()).thenReturn(customerId);
        when(slot.getDate()).thenReturn(LocalDate.now(VENUE_ZONE).minusDays(1));

        assertThrows(ConflictException.class,
                () -> service.checkIn(customerId, VenueCheckInService.VENUE_CODE, "JNZ-TEST1234"));
        verifyNoInteractions(users);
    }
}
