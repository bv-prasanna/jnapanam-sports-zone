package com.jnapanam.sportszone.catalog.dto;

import com.jnapanam.sportszone.catalog.model.CatalogueStatus;
import com.jnapanam.sportszone.catalog.model.Facility;
import com.jnapanam.sportszone.catalog.model.Sport;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FacilityResponseStatusTest {
    @Test void parentSuspensionIsShownWithoutOverwritingFacilityStatus() {
        Sport sport = new Sport("BADMINTON", "Badminton", null, null, 1);
        Facility facility = new Facility(sport, "COURT1", "Court 1", null, null, 60, 2);
        LocalDate today = LocalDate.now();
        sport.changeStatus(CatalogueStatus.TEMPORARILY_SUSPENDED, today, today.plusDays(5), "Maintenance");
        assertEquals("TEMPORARILY_SUSPENDED", FacilityResponse.from(facility).status());
        assertEquals(CatalogueStatus.ACTIVE, facility.getStatus());
        assertFalse(facility.isAvailableOn(today));
        sport.changeStatus(CatalogueStatus.ACTIVE, null, null, null);
        assertEquals("ACTIVE", FacilityResponse.from(facility).status());
    }
    @Test void inactiveParentOverridesIndependentlySuspendedFacility() {
        Sport sport = new Sport("SWIMMING", "Swimming", null, null, 1);
        Facility facility = new Facility(sport, "POOL1", "Pool", null, null, 60, 2);
        LocalDate today = LocalDate.now();
        facility.changeStatus(CatalogueStatus.TEMPORARILY_SUSPENDED, today, today.plusDays(10), "Local repair");
        sport.changeStatus(CatalogueStatus.INACTIVE, null, null, "No longer offered");
        assertEquals("INACTIVE", FacilityResponse.from(facility).status());
        sport.changeStatus(CatalogueStatus.ACTIVE, null, null, null);
        assertEquals("TEMPORARILY_SUSPENDED", FacilityResponse.from(facility).status());
        assertFalse(facility.isAvailableOn(today));
    }
}
