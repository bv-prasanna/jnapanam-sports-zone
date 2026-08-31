package com.jnapanam.sportszone.catalog.repository;

import com.jnapanam.sportszone.catalog.model.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SportRepository extends JpaRepository<Sport, UUID> {
    Optional<Sport> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<Sport> findAllByOrderByDisplayOrderAscNameAsc();
    List<Sport> findByStatusOrderByDisplayOrderAscNameAsc(CatalogueStatus status);
}

