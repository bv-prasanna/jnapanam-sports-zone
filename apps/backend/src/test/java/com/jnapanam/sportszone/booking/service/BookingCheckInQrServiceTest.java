package com.jnapanam.sportszone.booking.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.jnapanam.sportszone.admin.service.OperatorAssignmentService;
import com.jnapanam.sportszone.booking.model.*;
import com.jnapanam.sportszone.booking.repository.BookingRepository;
import com.jnapanam.sportszone.catalog.model.*;
import com.jnapanam.sportszone.common.exception.*;
import com.jnapanam.sportszone.identity.model.User;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import com.jnapanam.sportszone.notification.service.NotificationService;
import java.time.*;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BookingCheckInQrServiceTest {
    private final BookingRepository bookings=mock(BookingRepository.class);
    private final OperatorAssignmentService assignments=mock(OperatorAssignmentService.class);
    private final UserRepository users=mock(UserRepository.class);
    private final NotificationService notifications=mock(NotificationService.class);
    private final User customer=mock(User.class);
    private final User operator=mock(User.class);
    private final Booking booking=mock(Booking.class);
    private final BookingSlot slot=mock(BookingSlot.class);
    private final Facility facility=mock(Facility.class);
    private final Sport sport=mock(Sport.class);
    private final UUID bookingId=UUID.randomUUID(),userId=UUID.randomUUID(),operatorId=UUID.randomUUID();
    private final UUID slotId=UUID.randomUUID();
    private final BookingCheckInQrService service=new BookingCheckInQrService(
            bookings,assignments,users,notifications,
            new SecretKeySpec("secret-at-least-thirty-two-bytes-2026".getBytes(),"HmacSHA256"));

    @BeforeEach void setup() {
        when(booking.getId()).thenReturn(bookingId);
        when(booking.getBookingNumber()).thenReturn("JNZ-TEST1234");
        when(booking.getUser()).thenReturn(customer);
        when(customer.getId()).thenReturn(userId);
        when(customer.getFullName()).thenReturn("Customer");
        when(booking.getSlot()).thenReturn(slot);
        when(slot.getId()).thenReturn(slotId);
        when(slot.getFacility()).thenReturn(facility);
        when(facility.getSport()).thenReturn(sport);
        when(facility.getName()).thenReturn("Badminton Court 1");
        when(sport.getName()).thenReturn("Badminton");
        LocalDate date=LocalDate.now(ZoneId.of("Asia/Kolkata"));
        when(slot.getDate()).thenReturn(date);
        LocalTime time=LocalTime.now(ZoneId.of("Asia/Kolkata"));
        when(slot.getStartTime()).thenReturn(time.minusMinutes(10));
        when(slot.getEndTime()).thenReturn(time.plusMinutes(50));
        when(booking.getStatus()).thenReturn(BookingStatus.CONFIRMED);
        when(booking.getPaymentStatus()).thenReturn(BookingPaymentStatus.PAID);
        when(bookings.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookings.findLockedById(bookingId)).thenReturn(Optional.of(booking));
        when(users.findById(operatorId)).thenReturn(Optional.of(operator));
    }

    @Test void sameBookingPassIsStableAndContainsNoPersonalData() {
        String first=service.pass(userId,bookingId).value();
        assertEquals(first,service.pass(userId,bookingId).value());
        assertTrue(first.startsWith("JNZCI1."+bookingId+"."));
        assertFalse(first.contains("Customer"));
        assertFalse(first.contains("Badminton"));
    }

    @Test void onlyBookingOwnerCanObtainPass() {
        assertThrows(NotFoundException.class,()->service.pass(UUID.randomUUID(),bookingId));
    }

    @Test void scanMatchesBookingFacilityAndPaymentWithoutTrustingClientData() {
        String pass=service.pass(userId,bookingId).value();
        var result=service.verify(operatorId,pass);
        assertEquals("JNZ-TEST1234",result.bookingNumber());
        assertEquals("Badminton",result.sportName());
        assertEquals("Badminton Court 1",result.facilityName());
        assertEquals("PAID",result.paymentStatus());
        verify(assignments).assertBookingAllowed(operatorId,bookingId);
    }

    @Test void modifiedOrOutdatedPassIsRejected() {
        String pass=service.pass(userId,bookingId).value();
        assertThrows(BadRequestException.class,
                ()->service.verify(operatorId,pass.substring(0,pass.length()-1)+(pass.endsWith("A")?"B":"A")));
        when(slot.getDate()).thenReturn(LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1));
        assertThrows(BadRequestException.class,()->service.verify(operatorId,pass));
    }

    @Test void paymentAndCancellationAreRevalidatedEvenWithGenuinePass() {
        String pass=service.pass(userId,bookingId).value();
        when(booking.getPaymentStatus()).thenReturn(BookingPaymentStatus.UNPAID);
        assertThrows(ConflictException.class,()->service.verify(operatorId,pass));
        when(booking.getPaymentStatus()).thenReturn(BookingPaymentStatus.PAID);
        when(booking.getStatus()).thenReturn(BookingStatus.CANCELLED);
        assertThrows(ConflictException.class,()->service.verify(operatorId,pass));
    }

    @Test void qrCheckInRequiresHumanIdentityVerificationAndNotifiesCustomer() {
        String pass=service.pass(userId,bookingId).value();
        assertThrows(BadRequestException.class,()->service.checkIn(operatorId,pass,false));
        service.checkIn(operatorId,pass,true);
        verify(bookings).findLockedById(bookingId);
        verify(booking).checkIn(eq(operator),eq(true),contains("QR"));
        verify(notifications).lifecycle(eq(userId),eq("BOOKING"),eq("Booking checked in"),anyString());
    }

    @Test void alreadyCheckedInQrCannotBeReused() {
        String pass=service.pass(userId,bookingId).value();
        when(booking.getCheckedInAt()).thenReturn(Instant.now());
        assertThrows(ConflictException.class,()->service.verify(operatorId,pass));
    }
}
