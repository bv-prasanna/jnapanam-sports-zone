package com.jnapanam.sportszone.catalog;

import com.jnapanam.sportszone.catalog.dto.*;
import com.jnapanam.sportszone.catalog.model.*;
import com.jnapanam.sportszone.catalog.repository.*;
import com.jnapanam.sportszone.catalog.service.CatalogueService;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CatalogueGeneratedCodeTest {
    @Test void sportCreationGeneratesImmutableCodeWithoutClientCode() {
        SportRepository sports = mock(SportRepository.class);
        FacilityRepository facilities = mock(FacilityRepository.class);
        when(sports.save(any(Sport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CatalogueService service = new CatalogueService(sports, facilities);
        var response = service.createSport(new SportRequest(null, "Badminton", "Indoor", "badminton", 0));
        assertNotNull(response);
        verify(sports).save(argThat(s -> s.getCode() != null && s.getCode().matches("BADMINTON_[A-F0-9]{12}")));
    }

    @Test void sportCreationDoesNotRequireClientSuppliedCode() {
        SportRepository sports = mock(SportRepository.class);
        FacilityRepository facilities = mock(FacilityRepository.class);
        when(sports.save(any(Sport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CatalogueService service = new CatalogueService(sports, facilities);
        service.createSport(new SportRequest(null, "Swimming", null, "swimming", 0));
        verify(sports).save(argThat(s -> s.getCode().startsWith("SWIMMING_")));
    }
}
