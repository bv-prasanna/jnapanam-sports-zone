package com.jnapanam.sportszone.catalog.repository;

import com.jnapanam.sportszone.catalog.model.FacilityOperatingHour;
import java.time.DayOfWeek;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityOperatingHourRepository extends JpaRepository<FacilityOperatingHour, UUID> {
    List<FacilityOperatingHour> findByFacilityIdAndDayOfWeekOrderByOpenTimeAsc(UUID facilityId, DayOfWeek dayOfWeek);
    List<FacilityOperatingHour> findByFacilityIdOrderByDayOfWeekAsc(UUID facilityId);
}

