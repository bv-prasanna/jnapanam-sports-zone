package com.jnapanam.sportszone.booking.controller;
import com.jnapanam.sportszone.booking.dto.*;
import com.jnapanam.sportszone.booking.service.BookingService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/bookings")
public class BookingController{
 private final BookingService service; public BookingController(BookingService service){this.service=service;}
 @PostMapping("/holds") @ResponseStatus(HttpStatus.CREATED) public HoldResponse hold(Authentication auth,@Valid @RequestBody HoldRequest request){return service.hold(userId(auth),request);}
 @DeleteMapping("/holds/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void release(Authentication auth,@PathVariable UUID id){service.release(userId(auth),id);}
 @PostMapping("/confirm") @ResponseStatus(HttpStatus.CREATED) public BookingResponse confirm(Authentication auth,@Valid @RequestBody ConfirmRequest request){return service.confirm(userId(auth),request);}
 @GetMapping("/me") public List<BookingResponse> mine(Authentication auth){return service.mine(userId(auth));}
 @PostMapping("/{id}/cancel") public BookingResponse cancel(Authentication auth,@PathVariable UUID id){return service.cancel(userId(auth),id);}
 private static UUID userId(Authentication auth){return UUID.fromString(auth.getName());}
}
