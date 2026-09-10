package com.jnapanam.sportszone.booking.controller;
import com.jnapanam.sportszone.booking.dto.BookingResponse;
import com.jnapanam.sportszone.booking.service.BookingService;
import java.time.LocalDate;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin/bookings") @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OPERATIONS')")
public class AdminBookingController{
 private final BookingService service; public AdminBookingController(BookingService service){this.service=service;}
 @GetMapping public List<BookingResponse> list(
  @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,
  @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
  @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
  @RequestParam(required=false) String sport,
  @RequestParam(required=false) UUID facilityId,
  @RequestParam(required=false) String status,
  @RequestParam(required=false) String paymentStatus,
  @RequestParam(required=false,name="q") String query){
   return service.adminList(date,from,to,sport,facilityId,status,paymentStatus,query);
 }
}
