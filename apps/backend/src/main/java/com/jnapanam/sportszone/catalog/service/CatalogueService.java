package com.jnapanam.sportszone.catalog.service;

import com.jnapanam.sportszone.catalog.dto.*;
import com.jnapanam.sportszone.catalog.model.*;
import com.jnapanam.sportszone.catalog.repository.*;
import com.jnapanam.sportszone.common.exception.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogueService {
    private final SportRepository sports;
    private final FacilityRepository facilities;

    public CatalogueService(SportRepository sports, FacilityRepository facilities) {
        this.sports = sports;
        this.facilities = facilities;
    }

    @Transactional(readOnly = true)
    public List<SportResponse> publicSports() {
        return sports.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .filter(sport -> sport.getStatus() != CatalogueStatus.INACTIVE)
                .map(SportResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SportResponse> allSports() {
        return sports.findAllByOrderByDisplayOrderAscNameAsc().stream().map(SportResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SportResponse sport(String code) { return SportResponse.from(requireSport(code)); }

    @Transactional(readOnly = true)
    public SportResponse publicSport(String code) {
        Sport sport = requireSport(code);
        if (sport.getStatus() == CatalogueStatus.INACTIVE) throw new NotFoundException("Sport not found");
        return SportResponse.from(sport);
    }

    @Transactional
    public SportResponse createSport(SportRequest request) {
        if (sports.existsByCodeIgnoreCase(request.code())) throw new ConflictException("Sport code already exists");
        return SportResponse.from(sports.save(new Sport(request.code(), request.name(), request.description(), request.iconKey(), request.displayOrder())));
    }

    @Transactional
    public SportResponse updateSport(UUID id, SportRequest request) {
        Sport sport = requireSport(id);
        if (!sport.getCode().equalsIgnoreCase(request.code())) throw new BadRequestException("Sport code cannot be changed");
        sport.update(request.name(), request.description(), request.iconKey(), request.displayOrder());
        return SportResponse.from(sport);
    }

    @Transactional
    public SportResponse changeSportStatus(UUID id, StatusRequest request) {
        validateStatus(request);
        Sport sport = requireSport(id);
        sport.changeStatus(request.status(), request.suspendedFrom(), request.suspendedUntil(), request.reason());
        return SportResponse.from(sport);
    }

    @Transactional(readOnly = true)
    public List<FacilityResponse> publicFacilities(String sportCode, LocalDate date) {
        LocalDate effectiveDate = date == null ? LocalDate.now() : date;
        List<Facility> values = sportCode == null || sportCode.isBlank()
                ? facilities.findAllByOrderByNameAsc()
                : facilities.findBySportCodeIgnoreCaseOrderByNameAsc(sportCode);
        return values.stream().filter(facility -> facility.getStatus() != CatalogueStatus.INACTIVE)
                .filter(facility -> facility.isAvailableOn(effectiveDate))
                .map(FacilityResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<FacilityResponse> allFacilities() {
        return facilities.findAllByOrderByNameAsc().stream().map(FacilityResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public FacilityResponse facility(String code) { return FacilityResponse.from(requireFacility(code)); }

    @Transactional(readOnly = true)
    public FacilityResponse publicFacility(String code) {
        Facility facility = requireFacility(code);
        if (facility.getStatus() == CatalogueStatus.INACTIVE || facility.getSport().getStatus() == CatalogueStatus.INACTIVE)
            throw new NotFoundException("Facility not found");
        return FacilityResponse.from(facility);
    }

    @Transactional
    public FacilityResponse createFacility(FacilityRequest request) {
        if (facilities.existsByCodeIgnoreCase(request.code())) throw new ConflictException("Facility code already exists");
        Sport sport = requireSport(request.sportCode());
        Facility facility = new Facility(sport, request.code(), request.name(), request.description(), request.locationLabel(), request.slotDurationMinutes(), request.capacity(), request.bookingPrice());
        return FacilityResponse.from(facilities.save(facility));
    }

    @Transactional
    public FacilityResponse updateFacility(UUID id, FacilityRequest request) {
        Facility facility = requireFacility(id);
        if (!facility.getCode().equalsIgnoreCase(request.code())) throw new BadRequestException("Facility code cannot be changed");
        facility.update(requireSport(request.sportCode()), request.name(), request.description(), request.locationLabel(), request.slotDurationMinutes(), request.capacity(), request.bookingPrice());
        return FacilityResponse.from(facility);
    }

    @Transactional
    public FacilityResponse changeFacilityStatus(UUID id, StatusRequest request) {
        validateStatus(request);
        Facility facility = requireFacility(id);
        facility.changeStatus(request.status(), request.suspendedFrom(), request.suspendedUntil(), request.reason());
        return FacilityResponse.from(facility);
    }

    public Sport requireSport(UUID id) { return sports.findById(id).orElseThrow(() -> new NotFoundException("Sport not found")); }
    public Sport requireSport(String code) { return sports.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("Sport not found")); }
    public Facility requireFacility(UUID id) { return facilities.findById(id).orElseThrow(() -> new NotFoundException("Facility not found")); }
    public Facility requireFacility(String code) { return facilities.findByCodeIgnoreCase(code).orElseThrow(() -> new NotFoundException("Facility not found")); }

    static void validateStatus(StatusRequest request) {
        if (request.status() == CatalogueStatus.TEMPORARILY_SUSPENDED) {
            if (request.suspendedFrom() == null || request.suspendedUntil() == null)
                throw new BadRequestException("Temporary suspension requires suspendedFrom and suspendedUntil");
            if (request.suspendedUntil().isBefore(request.suspendedFrom()))
                throw new BadRequestException("suspendedUntil must be on or after suspendedFrom");
        }
    }
}
