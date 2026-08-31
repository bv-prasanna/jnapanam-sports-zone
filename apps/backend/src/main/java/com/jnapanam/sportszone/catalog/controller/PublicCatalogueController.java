package com.jnapanam.sportszone.catalog.controller;

import com.jnapanam.sportszone.catalog.dto.*;
import com.jnapanam.sportszone.catalog.service.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public")
public class PublicCatalogueController {
    private final CatalogueService catalogue;
    private final FacilityScheduleService schedules;

    public PublicCatalogueController(CatalogueService catalogue, FacilityScheduleService schedules) {
        this.catalogue = catalogue; this.schedules = schedules;
    }

    @GetMapping("/sports")
    public List<SportResponse> sports() { return catalogue.publicSports(); }

    @GetMapping("/sports/{code}")
    public SportResponse sport(@PathVariable String code) { return catalogue.publicSport(code); }

    @GetMapping("/facilities")
    public List<FacilityResponse> facilities(@RequestParam(required = false) String sportCode,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return catalogue.publicFacilities(sportCode, date);
    }

    @GetMapping("/facilities/{code}")
    public FacilityResponse facility(@PathVariable String code) { return catalogue.publicFacility(code); }

    @GetMapping("/facilities/{facilityId}/schedule")
    public FacilityScheduleResponse schedule(@PathVariable UUID facilityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return schedules.schedule(facilityId, from, to);
    }
}
