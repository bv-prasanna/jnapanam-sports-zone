package com.jnapanam.sportszone.catalog.repository;

import com.jnapanam.sportszone.catalog.model.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityRepository extends JpaRepository<Facility, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select f from Facility f where f.id=:id")
    Optional<Facility> findLocked(@org.springframework.data.repository.query.Param("id") UUID id);

    Optional<Facility> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<Facility> findAllByOrderByNameAsc();
    List<Facility> findBySportCodeIgnoreCaseOrderByNameAsc(String sportCode);
    List<Facility> findByStatusOrderByNameAsc(CatalogueStatus status);
    List<Facility> findBySportCodeIgnoreCaseAndStatusOrderByNameAsc(String sportCode, CatalogueStatus status);
}

