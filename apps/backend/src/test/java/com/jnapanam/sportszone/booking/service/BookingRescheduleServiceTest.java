package com.jnapanam.sportszone.booking.service;

import com.jnapanam.sportszone.booking.model.Booking;
import com.jnapanam.sportszone.booking.model.BookingSlot;
import com.jnapanam.sportszone.booking.model.BookingStatus;
import com.jnapanam.sportszone.booking.repository.BookingRepository;
import com.jnapanam.sportszone.booking.repository.BookingSlotRepository;
import com.jnapanam.sportszone.catalog.model.Facility;
import com.jnapanam.sportszone.common.exception.ConflictException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class BookingRescheduleServiceTest {
    private BookingRepository bookings;
    private BookingSlotRepository slots;
    private JdbcTemplate jdbc;
    private BookingRescheduleService service;

    @BeforeEach
    void setUp() {
        bookings = mock(BookingRepository.class);
        slots = mock(BookingSlotRepository.class);
        jdbc = mock(JdbcTemplate.class);
        service = new BookingRescheduleService(bookings, slots, jdbc);
    }

    @Test
    void rejectsRescheduleToSameSlotBeforeChangingCapacity() {
        UUID actor = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        Booking booking = mock(Booking.class);
        BookingSlot existing = mock(BookingSlot.class);
        when(bookings.findById(bookingId)).thenReturn(Optional.of(booking));
        when(booking.getStatus()).thenReturn(BookingStatus.CONFIRMED);
        when(booking.getSlot()).thenReturn(existing);
        when(existing.getId()).thenReturn(slotId);

        assertThrows(ConflictException.class,
                () -> service.reschedule(actor, bookingId, slotId, true));
        verifyNoInteractions(slots, jdbc);
    }

    @Test
    void rejectsCheckedInBookingsBeforeChangingCapacity() {
        UUID actor = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Booking booking = mock(Booking.class);
        when(bookings.findById(bookingId)).thenReturn(Optional.of(booking));
        when(booking.getStatus()).thenReturn(BookingStatus.CONFIRMED);
        when(booking.getCheckedInAt()).thenReturn(java.time.Instant.now());

        assertThrows(ConflictException.class,
                () -> service.reschedule(actor, bookingId, UUID.randomUUID(), true));
        verifyNoInteractions(slots, jdbc);
    }

    @Test
    void rejectsUnavailableTargetFacilityBeforeChangingCapacity() {
        UUID actor = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID oldId = UUID.randomUUID();
        UUID newId = UUID.randomUUID();
        Booking booking = mock(Booking.class);
        BookingSlot oldSlot = mock(BookingSlot.class);
        BookingSlot newSlot = mock(BookingSlot.class);
        Facility facility = mock(Facility.class);
        LocalDate date = LocalDate.now().plusDays(2);
        when(bookings.findById(bookingId)).thenReturn(Optional.of(booking));
        when(booking.getStatus()).thenReturn(BookingStatus.CONFIRMED);
        when(booking.getSlot()).thenReturn(oldSlot);
        when(oldSlot.getId()).thenReturn(oldId);
        when(slots.findLocked(oldId)).thenReturn(Optional.of(oldSlot));
        when(slots.findLocked(newId)).thenReturn(Optional.of(newSlot));
        when(newSlot.getFacility()).thenReturn(facility);
        when(newSlot.getDate()).thenReturn(date);
        when(facility.isAvailableOn(date)).thenReturn(false);

        assertThrows(ConflictException.class,
                () -> service.reschedule(actor, bookingId, newId, true));
        verify(oldSlot, never()).cancel(anyInt());
        verify(newSlot, never()).hold(anyInt());
        verifyNoInteractions(jdbc);
    }
}
