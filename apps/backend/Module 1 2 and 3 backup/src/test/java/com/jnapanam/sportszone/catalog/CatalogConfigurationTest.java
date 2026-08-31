package com.jnapanam.sportszone.catalog;

import static org.junit.jupiter.api.Assertions.*;

import com.jnapanam.sportszone.catalog.model.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CatalogConfigurationTest {
    @Test
    void temporarySportSuspensionAppliesOnlyInsideConfiguredDates() {
        Sport sport = new Sport("SWIMMING", "Swimming", null, "swim", 1);
        sport.changeStatus(CatalogueStatus.TEMPORARILY_SUSPENDED,
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 20), "Maintenance");

        assertTrue(sport.isAvailableOn(LocalDate.of(2026, 9, 9)));
        assertFalse(sport.isAvailableOn(LocalDate.of(2026, 9, 10)));
        assertFalse(sport.isAvailableOn(LocalDate.of(2026, 9, 20)));
        assertTrue(sport.isAvailableOn(LocalDate.of(2026, 9, 21)));
    }

    @Test
    void inactiveSportMakesItsFacilityUnavailable() {
        Sport sport = new Sport("BADMINTON", "Badminton", null, "badminton", 1);
        Facility court = new Facility(sport, "BAD-01", "Court 1", null, "Indoor block", 60, 4);
        sport.changeStatus(CatalogueStatus.INACTIVE, null, null, "Service discontinued");

        assertFalse(court.isAvailableOn(LocalDate.now()));
    }
}
