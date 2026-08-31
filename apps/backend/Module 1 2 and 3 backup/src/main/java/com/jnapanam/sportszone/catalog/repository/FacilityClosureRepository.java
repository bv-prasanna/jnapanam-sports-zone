package com.jnapanam.sportszone.catalog.repository;

import com.jnapanam.sportszone.catalog.model.FacilityClosure;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityClosureRepository extends JpaRepository<FacilityClosure, UUID> {
    boolean existsByFacilityIdAndClosureDate(UUID facilityId, LocalDate date);
    List<FacilityClosure> findByFacilityIdAndClosureDateBetweenOrderByClosureDateAsc(UUID facilityId, LocalDate from, LocalDate to);
}

