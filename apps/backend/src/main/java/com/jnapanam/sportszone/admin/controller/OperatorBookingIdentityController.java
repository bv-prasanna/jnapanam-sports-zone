package com.jnapanam.sportszone.admin.controller;

import com.jnapanam.sportszone.admin.service.OperatorAssignmentService;
import com.jnapanam.sportszone.booking.repository.BookingRepository;
import com.jnapanam.sportszone.common.exception.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Identity evidence may be read only by the operator assigned to the booking
 * (or an administrator). Images/document bytes are never embedded in booking lists,
 * QR codes, URL query parameters, or logs.
 */
@RestController
@RequestMapping("/api/v1/operations/bookings/{bookingNumber}/identity")
@PreAuthorize("hasAnyRole('OPERATIONS','ADMIN','SUPER_ADMIN')")
public class OperatorBookingIdentityController {
    private final OperatorAssignmentService assignments;
    private final BookingRepository bookings;
    private final JdbcTemplate jdbc;

    public OperatorBookingIdentityController(OperatorAssignmentService assignments,
            BookingRepository bookings, JdbcTemplate jdbc) {
        this.assignments=assignments;this.bookings=bookings;this.jdbc=jdbc;
    }

    public record IdentityView(String customerName,boolean photoAvailable,String profilePhotoUrl,
            String documentType,String documentStatus,boolean documentAvailable) {}

    private UUID allowedUser(Authentication auth,String number) {
        assignments.assertBookingAllowed(UUID.fromString(auth.getName()),number);
        return bookings.findByBookingNumberIgnoreCase(number)
                .orElseThrow(()->new NotFoundException("Booking not found")).getUser().getId();
    }

    @GetMapping
    public IdentityView status(Authentication auth,@PathVariable String bookingNumber) {
        UUID userId=allowedUser(auth,bookingNumber);
        var customer=bookings.findByBookingNumberIgnoreCase(bookingNumber)
                .orElseThrow(()->new NotFoundException("Booking not found")).getUser();
        String name=customer.getFullName();
        Integer photo=jdbc.queryForObject("select count(*) from user_profile_photos where user_id=?",Integer.class,userId);
        var rows=jdbc.queryForList("select document_type,status from booking_address_proofs where user_id=? order by uploaded_at desc limit 1",userId);
        return new IdentityView(name,photo!=null&&photo>0,customer.getProfilePhotoUrl(),
                rows.isEmpty()?null:(String)rows.get(0).get("document_type"),
                rows.isEmpty()?null:(String)rows.get(0).get("status"),!rows.isEmpty());
    }

    @GetMapping("/photo")
    public ResponseEntity<byte[]> photo(Authentication auth,@PathVariable String bookingNumber) {
        UUID userId=allowedUser(auth,bookingNumber);
        List<Map<String,Object>> rows=jdbc.queryForList("select media_type,photo_bytes from user_profile_photos where user_id=?",userId);
        if(rows.isEmpty())throw new NotFoundException("Profile photo has not been uploaded");
        return protectedBytes((String)rows.get(0).get("media_type"),(byte[])rows.get(0).get("photo_bytes"));
    }

    @GetMapping("/document")
    public ResponseEntity<byte[]> document(Authentication auth,@PathVariable String bookingNumber) {
        UUID userId=allowedUser(auth,bookingNumber);
        List<Map<String,Object>> rows=jdbc.queryForList("select content_type,document_bytes from booking_address_proofs where user_id=? order by uploaded_at desc limit 1",userId);
        if(rows.isEmpty())throw new NotFoundException("Customer document has not been uploaded");
        return protectedBytes((String)rows.get(0).get("content_type"),(byte[])rows.get(0).get("document_bytes"));
    }

    private static ResponseEntity<byte[]> protectedBytes(String contentType,byte[] data) {
        if(contentType==null||!List.of("image/jpeg","image/png","image/webp","application/pdf").contains(contentType))
            throw new NotFoundException("Unsupported identity media");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=identity-evidence")
                .contentType(MediaType.parseMediaType(contentType)).body(data);
    }
}
