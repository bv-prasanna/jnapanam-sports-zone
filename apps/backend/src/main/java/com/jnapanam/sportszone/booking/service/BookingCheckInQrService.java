package com.jnapanam.sportszone.booking.service;

import com.jnapanam.sportszone.admin.service.OperatorAssignmentService;
import com.jnapanam.sportszone.booking.dto.BookingResponse;
import com.jnapanam.sportszone.booking.model.*;
import com.jnapanam.sportszone.booking.repository.BookingRepository;
import com.jnapanam.sportszone.common.exception.*;
import com.jnapanam.sportszone.identity.repository.UserRepository;
import com.jnapanam.sportszone.notification.service.NotificationService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One deterministic HMAC pass per booking/slot. No customer PII in QR payload,
 * no extra row/QR generation on repeated app opens. The HMAC is invalidated on
 * slot changes; check-in is single-use via locked booking.checkedInAt.
 */
@Service
public class BookingCheckInQrService {
    private static final String PREFIX="JNZCI1";
    private static final ZoneId VENUE_ZONE=ZoneId.of("Asia/Kolkata");
    private final BookingRepository bookings;
    private final OperatorAssignmentService assignments;
    private final UserRepository users;
    private final NotificationService notifications;
    private final SecretKey signingKey;

    public BookingCheckInQrService(BookingRepository bookings,
            OperatorAssignmentService assignments, UserRepository users,
            NotificationService notifications, SecretKey signingKey) {
        this.bookings=bookings;this.assignments=assignments;this.users=users;
        this.notifications=notifications;this.signingKey=signingKey;
    }

    public record Pass(String bookingNumber,String value,String date,String startTime,String facilityName) {}
    public record Scan(String bookingNumber,String customerName,String sportName,
            String facilityName,String date,String startTime,String endTime,
            String paymentStatus,boolean checkedIn,String profilePhotoUrl) {}

    @Transactional(readOnly=true)
    public Pass pass(UUID customerId,UUID bookingId) {
        Booking booking=bookings.findById(bookingId)
                .orElseThrow(()->new NotFoundException("Booking not found"));
        if(!booking.getUser().getId().equals(customerId))throw new NotFoundException("Booking not found");
        requireActivePaid(booking);
        if(booking.getCheckedInAt()!=null)throw new ConflictException("This booking is already checked in");
        return new Pass(booking.getBookingNumber(),sign(booking),booking.getSlot().getDate().toString(),
                booking.getSlot().getStartTime().toString(),booking.getSlot().getFacility().getName());
    }

    @Transactional(readOnly=true)
    public Scan verify(UUID operatorId,String qr) {
        Booking booking=parseAndAuthorize(operatorId,qr,false);
        return scan(booking);
    }

    @Transactional
    public BookingResponse checkIn(UUID operatorId,String qr,boolean identityVerified) {
        if(!identityVerified)throw new BadRequestException("Verify the customer photo/document before QR check-in");
        // Pessimistic lock makes a reused QR single-use even under two simultaneous scans.
        Booking booking=parseAndAuthorize(operatorId,qr,true);
        booking.checkIn(users.findById(operatorId).orElseThrow(()->new NotFoundException("Operator not found")),
                true,"Booking QR and identity verified by operator");
        notifications.lifecycle(booking.getUser().getId(),"BOOKING","Booking checked in",
                "Checked in for booking "+booking.getBookingNumber()+".");
        return BookingResponse.from(booking);
    }

    private Booking parseAndAuthorize(UUID operatorId,String value,boolean lock) {
        if(value==null||!value.matches("JNZCI1\\.[0-9a-fA-F-]{36}\\.[A-Za-z0-9_-]{43}"))
            throw new BadRequestException("Invalid booking check-in QR");
        String[] parts=value.split("\\.");
        UUID id;
        try{id=UUID.fromString(parts[1]);}catch(IllegalArgumentException e){throw new BadRequestException("Invalid booking check-in QR");}
        Booking booking=(lock?bookings.findLockedById(id):bookings.findById(id))
                .orElseThrow(()->new NotFoundException("Booking not found"));
        assignments.assertBookingAllowed(operatorId,booking.getId());
        if(!MessageDigest.isEqual(sign(booking).getBytes(StandardCharsets.US_ASCII),
                value.getBytes(StandardCharsets.US_ASCII)))
            throw new BadRequestException("Invalid or outdated QR. Ask customer to refresh the pass.");
        requireActivePaid(booking);
        if(booking.getCheckedInAt()!=null)throw new ConflictException("Booking has already been checked in");
        LocalDateTime now=LocalDateTime.now(VENUE_ZONE);
        LocalDateTime start=LocalDateTime.of(booking.getSlot().getDate(),booking.getSlot().getStartTime());
        LocalDateTime end=LocalDateTime.of(booking.getSlot().getDate(),booking.getSlot().getEndTime());
        if(now.isBefore(start.minusMinutes(30)))throw new ConflictException("Check-in opens 30 minutes before the booking");
        if(now.isAfter(end))throw new ConflictException("The check-in window is closed");
        return booking;
    }

    private static void requireActivePaid(Booking booking) {
        if(booking.getStatus()!=BookingStatus.CONFIRMED)
            throw new ConflictException("Only confirmed bookings can be checked in");
        if(booking.getPaymentStatus()!=BookingPaymentStatus.PAID
                && booking.getPaymentStatus()!=BookingPaymentStatus.NOT_REQUIRED)
            throw new ConflictException("Payment is not confirmed for this booking");
    }

    private Scan scan(Booking b) {
        var slot=b.getSlot();
        var photo=b.getUser().getProfilePhotoUrl();
        return new Scan(b.getBookingNumber(),b.getUser().getFullName(),
                slot.getFacility().getSport().getName(),slot.getFacility().getName(),
                slot.getDate().toString(),slot.getStartTime().toString(),
                slot.getEndTime().toString(),b.getPaymentStatus().name(),
                b.getCheckedInAt()!=null,photo);
    }

    private String sign(Booking booking) {
        try{
            Mac mac=Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            String message=PREFIX+"|"+booking.getId()+"|"+booking.getUser().getId()+"|"+
                    booking.getSlot().getId()+"|"+booking.getSlot().getDate();
            String digest=Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
            return PREFIX+"."+booking.getId()+"."+digest;
        }catch(Exception e){throw new IllegalStateException("Unable to sign booking QR",e);}
    }
}
