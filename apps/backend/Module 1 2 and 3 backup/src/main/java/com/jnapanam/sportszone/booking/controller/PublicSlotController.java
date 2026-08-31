package com.jnapanam.sportszone.booking.controller;
import com.jnapanam.sportszone.booking.dto.SlotResponse;
import com.jnapanam.sportszone.booking.service.SlotService;
import java.time.LocalDate;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/public/facilities")
public class PublicSlotController{
 private final SlotService service; public PublicSlotController(SlotService service){this.service=service;}
 @GetMapping("/{facilityId}/slots") public List<SlotResponse> slots(@PathVariable UUID facilityId,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date){return service.availability(facilityId,date);}
}
