package com.jnapanam.sportszone.catalog.repository;

import com.jnapanam.sportszone.catalog.model.FacilityScheduleOverride;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityScheduleOverrideRepository extends JpaRepository<FacilityScheduleOverride, UUID> {
    List<FacilityScheduleOverride> findByFacilityIdAndScheduleDateOrderByStartTimeAsc(UUID facilityId, LocalDate date);
    List<FacilityScheduleOverride> findByFacilityIdAndScheduleDateBetweenOrderByScheduleDateAscStartTimeAsc(UUID facilityId, LocalDate from, LocalDate to);
}

