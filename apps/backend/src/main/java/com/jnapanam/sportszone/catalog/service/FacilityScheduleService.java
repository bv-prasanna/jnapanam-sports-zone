package com.jnapanam.sportszone.catalog.service;

import com.jnapanam.sportszone.catalog.dto.*;
import com.jnapanam.sportszone.catalog.model.*;
import com.jnapanam.sportszone.catalog.repository.*;
import com.jnapanam.sportszone.common.exception.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FacilityScheduleService {
    private final CatalogueService catalogue;
    private final FacilityOperatingHourRepository hours;
    private final FacilityClosureRepository closures;
    private final FacilityScheduleOverrideRepository overrides;

    public FacilityScheduleService(CatalogueService catalogue, FacilityOperatingHourRepository hours,
                                   FacilityClosureRepository closures, FacilityScheduleOverrideRepository overrides) {
        this.catalogue = catalogue; this.hours = hours; this.closures = closures; this.overrides = overrides;
    }

    /** Backward compatible single-period endpoint: replaces that weekday, never appends silently. */
    @Transactional
    public OperatingHourResponse setOperatingHour(UUID facilityId, OperatingHourRequest request) {
        return replaceOperatingPeriods(facilityId, request.dayOfWeek(), List.of(request)).get(0);
    }

    /** Atomically replace all non-overlapping operating windows for one weekday. */
    @Transactional
    public List<OperatingHourResponse> replaceOperatingPeriods(UUID facilityId, DayOfWeek day,
                                                                List<OperatingHourRequest> requests) {
        Facility facility = catalogue.requireFacilityLocked(facilityId);
        if (day == null || requests == null || requests.isEmpty())
            throw new BadRequestException("Specify at least one operating period or a closed weekday");
        for (OperatingHourRequest request : requests) {
            if (request == null || request.dayOfWeek() != day)
                throw new BadRequestException("Every period must use the selected weekday");
            validateHours(request);
        }
        List<OperatingHourRequest> sorted = requests.stream()
                .sorted(Comparator.comparing(r -> r.closed() ? LocalTime.MIN : r.openTime())).toList();
        if (sorted.stream().anyMatch(OperatingHourRequest::closed) && sorted.size() != 1)
            throw new BadRequestException("A closed weekday cannot also have opening periods");
        for (int i = 1; i < sorted.size(); i++)
            if (sorted.get(i - 1).closeTime().isAfter(sorted.get(i).openTime()))
                throw new BadRequestException("Operating periods cannot overlap");
        hours.deleteAll(hours.findByFacilityIdAndDayOfWeekOrderByOpenTimeAsc(facilityId, day));
        hours.flush();
        List<FacilityOperatingHour> replacements = sorted.stream()
                .map(r -> new FacilityOperatingHour(facility, day, r.openTime(), r.closeTime(), r.closed())).toList();
        return hours.saveAll(replacements).stream().map(OperatingHourResponse::from).toList();
    }

    @Transactional
    public ClosureResponse addClosure(UUID facilityId, ClosureRequest request) {
        Facility facility = catalogue.requireFacility(facilityId);
        if (closures.existsByFacilityIdAndClosureDate(facilityId, request.closureDate()))
            throw new ConflictException("A closure already exists for this date");
        return ClosureResponse.from(closures.save(new FacilityClosure(facility, request.closureDate(), request.reason())));
    }

    @Transactional
    public void deleteClosure(UUID facilityId, UUID closureId) {
        catalogue.requireFacility(facilityId);
        FacilityClosure closure = closures.findById(closureId).orElseThrow(() -> new NotFoundException("Closure not found"));
        if (!closure.getFacility().getId().equals(facilityId)) throw new NotFoundException("Closure not found");
        closures.delete(closure);
    }

    @Transactional
    public ScheduleOverrideResponse addOverride(UUID facilityId, ScheduleOverrideRequest request) {
        if (!request.endTime().isAfter(request.startTime())) throw new BadRequestException("endTime must be after startTime");
        Facility facility = catalogue.requireFacility(facilityId);
        FacilityScheduleOverride value = new FacilityScheduleOverride(facility, request.scheduleDate(), request.startTime(), request.endTime(),
                request.availability(), request.slotDurationMinutes(), request.capacity(), request.reason());
        return ScheduleOverrideResponse.from(overrides.save(value));
    }

    @Transactional
    public void deleteOverride(UUID facilityId, UUID overrideId) {
        catalogue.requireFacility(facilityId);
        FacilityScheduleOverride value = overrides.findById(overrideId).orElseThrow(() -> new NotFoundException("Schedule override not found"));
        if (!value.getFacility().getId().equals(facilityId)) throw new NotFoundException("Schedule override not found");
        overrides.delete(value);
    }

    @Transactional(readOnly = true)
    public FacilityScheduleResponse schedule(UUID facilityId, LocalDate from, LocalDate to) {
        catalogue.requireFacility(facilityId);
        LocalDate start = from == null ? LocalDate.now() : from;
        LocalDate end = to == null ? start.plusDays(30) : to;
        if (end.isBefore(start)) throw new BadRequestException("to must be on or after from");
        if (end.isAfter(start.plusDays(92))) throw new BadRequestException("Schedule range cannot exceed 93 days");
        return new FacilityScheduleResponse(start, end,
                hours.findByFacilityIdOrderByDayOfWeekAsc(facilityId).stream().map(OperatingHourResponse::from).toList(),
                closures.findByFacilityIdAndClosureDateBetweenOrderByClosureDateAsc(facilityId, start, end).stream().map(ClosureResponse::from).toList(),
                overrides.findByFacilityIdAndScheduleDateBetweenOrderByScheduleDateAscStartTimeAsc(facilityId, start, end).stream().map(ScheduleOverrideResponse::from).toList());
    }

    private static void validateHours(OperatingHourRequest request) {
        if (request.closed()) {
            if (request.openTime() != null || request.closeTime() != null)
                throw new BadRequestException("Closed days must not contain openTime or closeTime");
        } else if (request.openTime() == null || request.closeTime() == null || !request.closeTime().isAfter(request.openTime())) {
            throw new BadRequestException("Open days require closeTime after openTime");
        }
    }
}
