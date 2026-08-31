package com.jnapanam.sportszone.catalog.repository;

import com.jnapanam.sportszone.catalog.model.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityRepository extends JpaRepository<Facility, UUID> {
    Optional<Facility> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<Facility> findAllByOrderByNameAsc();
    List<Facility> findBySportCodeIgnoreCaseOrderByNameAsc(String sportCode);
    List<Facility> findByStatusOrderByNameAsc(CatalogueStatus status);
    List<Facility> findBySportCodeIgnoreCaseAndStatusOrderByNameAsc(String sportCode, CatalogueStatus status);
}

