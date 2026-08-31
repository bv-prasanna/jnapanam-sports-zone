package com.jnapanam.sportszone.catalog.controller;

import com.jnapanam.sportszone.catalog.dto.*;
import com.jnapanam.sportszone.catalog.service.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/catalog")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','OPERATIONS')")
public class AdminCatalogueController {
    private final CatalogueService catalogue;
    private final FacilityScheduleService schedules;

    public AdminCatalogueController(CatalogueService catalogue, FacilityScheduleService schedules) {
        this.catalogue = catalogue; this.schedules = schedules;
    }

    @GetMapping("/sports") public List<SportResponse> sports() { return catalogue.allSports(); }
    @PostMapping("/sports") public ResponseEntity<SportResponse> createSport(@Valid @RequestBody SportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogue.createSport(request));
    }
    @PutMapping("/sports/{id}") public SportResponse updateSport(@PathVariable UUID id, @Valid @RequestBody SportRequest request) {
        return catalogue.updateSport(id, request);
    }
    @PatchMapping("/sports/{id}/status") public SportResponse changeSportStatus(@PathVariable UUID id, @Valid @RequestBody StatusRequest request) {
        return catalogue.changeSportStatus(id, request);
    }

    @GetMapping("/facilities") public List<FacilityResponse> facilities() { return catalogue.allFacilities(); }
    @PostMapping("/facilities") public ResponseEntity<FacilityResponse> createFacility(@Valid @RequestBody FacilityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogue.createFacility(request));
    }
    @PutMapping("/facilities/{id}") public FacilityResponse updateFacility(@PathVariable UUID id, @Valid @RequestBody FacilityRequest request) {
        return catalogue.updateFacility(id, request);
    }
    @PatchMapping("/facilities/{id}/status") public FacilityResponse changeFacilityStatus(@PathVariable UUID id, @Valid @RequestBody StatusRequest request) {
        return catalogue.changeFacilityStatus(id, request);
    }

    @PutMapping("/facilities/{facilityId}/operating-hours")
    public OperatingHourResponse setOperatingHour(@PathVariable UUID facilityId, @Valid @RequestBody OperatingHourRequest request) {
        return schedules.setOperatingHour(facilityId, request);
    }

    @PostMapping("/facilities/{facilityId}/closures")
    public ResponseEntity<ClosureResponse> addClosure(@PathVariable UUID facilityId, @Valid @RequestBody ClosureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schedules.addClosure(facilityId, request));
    }
    @DeleteMapping("/facilities/{facilityId}/closures/{closureId}")
    public ResponseEntity<Void> deleteClosure(@PathVariable UUID facilityId, @PathVariable UUID closureId) {
        schedules.deleteClosure(facilityId, closureId); return ResponseEntity.noContent().build();
    }

    @PostMapping("/facilities/{facilityId}/schedule-overrides")
    public ResponseEntity<ScheduleOverrideResponse> addOverride(@PathVariable UUID facilityId, @Valid @RequestBody ScheduleOverrideRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schedules.addOverride(facilityId, request));
    }
    @DeleteMapping("/facilities/{facilityId}/schedule-overrides/{overrideId}")
    public ResponseEntity<Void> deleteOverride(@PathVariable UUID facilityId, @PathVariable UUID overrideId) {
        schedules.deleteOverride(facilityId, overrideId); return ResponseEntity.noContent().build();
    }

    @GetMapping("/facilities/{facilityId}/schedule")
    public FacilityScheduleResponse schedule(@PathVariable UUID facilityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return schedules.schedule(facilityId, from, to);
    }
}

